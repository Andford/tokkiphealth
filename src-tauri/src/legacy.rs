// The old Tokkip Healthy PC (Electron, Andford/tokkiphealthypc): read once to
// bring your settings over, then replaced. Its data is only read (from
// %APPDATA%\Tokkip Healthy PC or %APPDATA%\tokkip-healthy-pc: config.json and
// history.json), never changed; once you say so, its uninstaller runs (it
// lives in Program Files, apart from this app, and keeps its data), and its
// startup task and its leftover Run entry go.

#![cfg_attr(not(windows), allow(dead_code))]

use crate::tools::{care::Routine, game::GameConfig};
use serde::Serialize;
use serde_json::Value;
use std::path::{Path, PathBuf};

/// What the old app's settings and history become here.
#[derive(Debug, Clone, Default, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Plan {
    /// "system", "light" or "dark", for the page.
    pub theme: Option<String>,
    /// "es" or "en", for the page.
    pub language: Option<String>,
    pub routine: Option<Routine>,
    pub game: Option<GameConfig>,
    /// What it freed, and how many cleans, added to what Tokkip has freed with you.
    pub freed: u64,
    pub cleans: u64,
    /// ms since 1970 of its last clean; 0 = none.
    pub last_clean: u64,
}

/// Where its data may be (electron-builder's product name, or the package name).
pub fn data_dirs(appdata: &Path) -> [PathBuf; 2] {
    [appdata.join("Tokkip Healthy PC"), appdata.join("tokkip-healthy-pc")]
}

/// Old game-mode optimisations → ours.
const GAME: [(&str, &str); 12] = [
    ("svc-wsearch", "search"),
    ("svc-sysmain", "sysmain"),
    ("svc-wuauserv", "update"),
    ("svc-spooler", "spooler"),
    ("svc-diagtrack", "telemetry"),
    ("proc-kill-browsers", "browsers"),
    ("proc-kill-chat", "chat"),
    ("proc-kill-updaters", "updaters"),
    ("sys-focus-assist", "notifications"),
    ("sys-power-plan", "power"),
    ("sys-prevent-sleep", "awake"),
    ("sys-disable-game-bar", "gamebar"),
];

/// ISO 8601 ("2026-10-02T09:30:00.000Z") → ms since 1970; 0 if it can't tell.
pub fn iso_ms(s: &str) -> u64 {
    let b = s.as_bytes();
    if b.len() < 19 {
        return 0;
    }
    let n = |r: std::ops::Range<usize>| s.get(r).and_then(|x| x.parse::<i64>().ok());
    let (Some(y), Some(mo), Some(d), Some(h), Some(mi), Some(se)) = (n(0..4), n(5..7), n(8..10), n(11..13), n(14..16), n(17..19)) else {
        return 0;
    };
    // Days from civil (Howard Hinnant).
    let (y, m) = if mo <= 2 { (y - 1, mo + 9) } else { (y, mo - 3) };
    let era = y.div_euclid(400);
    let yoe = y - era * 400;
    let doy = (153 * m + 2) / 5 + d - 1;
    let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    let days = era * 146_097 + doe - 719_468;
    let secs = days * 86_400 + h * 3600 + mi * 60 + se;
    if secs < 0 { 0 } else { secs as u64 * 1000 }
}

/// Reads the old config.json and history.json (either may be missing or broken).
pub fn plan(config: &str, history: &str) -> Plan {
    let mut p = Plan::default();
    let cfg: Value = serde_json::from_str(config).unwrap_or(Value::Null);
    let s = &cfg["settings"];
    if let Some(t) = s["theme"].as_str().filter(|t| ["system", "light", "dark"].contains(t)) {
        p.theme = Some(t.to_string());
    }
    if let Some(l) = s["language"].as_str().map(|l| l.get(..2).unwrap_or(l).to_lowercase()).filter(|l| l == "es" || l == "en") {
        p.language = Some(l);
    }
    // The weekly clean: the first schedule that's on (old days: 0 = Sunday; ours: 0 = Monday).
    let mut schedules: Vec<&Value> = s["schedules"].as_array().map(|a| a.iter().collect()).unwrap_or_default();
    schedules.push(&s["schedule"]);
    if let Some(e) = schedules.into_iter().find(|e| e["enabled"].as_bool() == Some(true)) {
        let weekly = e["frequency"].as_str() == Some("weekly");
        let day = if weekly { ((e["day"].as_u64().unwrap_or(1) % 7 + 6) % 7) as u8 } else { 0 };
        let hour = e["hour"].as_u64().unwrap_or(9).min(23) as u8;
        p.routine = Some(Routine { on: true, day, hour });
    }
    let g = &s["gameMode"];
    if g.is_object() {
        let strings = |v: &Value| -> Vec<String> { v.as_array().map(|a| a.iter().filter_map(|x| x.as_str().map(String::from)).collect()).unwrap_or_default() };
        let options: Vec<String> = strings(&g["enabledOptimizations"])
            .iter()
            .filter_map(|o| GAME.iter().find(|(old, _)| old == o).map(|(_, new)| new.to_string()))
            .collect();
        let games: Vec<String> = strings(&g["customGameProcesses"]).into_iter().filter(|x| !x.trim().is_empty() && x.len() < 80).collect();
        let mut config = GameConfig { auto: g["autoDetect"].as_bool().unwrap_or(false), games, ..GameConfig::default() };
        if !options.is_empty() {
            config.options = options;
        }
        p.game = Some(config);
    }
    let hist: Value = serde_json::from_str(history).unwrap_or(Value::Null);
    for e in hist.as_array().into_iter().flatten() {
        let saved = e["totalSpaceSaved"].as_u64().unwrap_or(0);
        if saved == 0 {
            continue;
        }
        p.freed += saved;
        p.cleans += 1;
        p.last_clean = p.last_clean.max(e["timestamp"].as_str().map(iso_ms).unwrap_or(0));
    }
    p
}

/// The old app's data folder, if it's there and hasn't been brought over yet.
pub fn pending(appdata: &Path, data: &Path) -> Option<PathBuf> {
    if data.join(DONE_FILE).exists() {
        return None;
    }
    data_dirs(appdata).into_iter().find(|d| d.join("config.json").is_file() || d.join("history.json").is_file())
}

pub const DONE_FILE: &str = "anterior.json";

pub fn read_plan(dir: &Path) -> Plan {
    let read = |n: &str| std::fs::read_to_string(dir.join(n)).unwrap_or_default();
    plan(&read("config.json"), &read("history.json"))
}

/// The old app's quiet uninstaller, when it's installed: (program, arguments).
#[cfg(windows)]
pub fn installed() -> Option<(String, Vec<String>)> {
    use winreg::enums::*;
    for hive in [HKEY_LOCAL_MACHINE, HKEY_CURRENT_USER] {
        for base in [r"Software\Microsoft\Windows\CurrentVersion\Uninstall", r"Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"] {
            let Ok(k) = winreg::RegKey::predef(hive).open_subkey(base) else { continue };
            for sub in k.enum_keys().flatten() {
                let Ok(s) = k.open_subkey(&sub) else { continue };
                let name: String = s.get_value("DisplayName").unwrap_or_default();
                let command: String = s.get_value("UninstallString").unwrap_or_default();
                if let Some(u) = old_uninstaller(&name, &command) {
                    return Some(u);
                }
            }
        }
    }
    None
}

#[cfg(not(windows))]
pub fn installed() -> Option<(String, Vec<String>)> {
    None
}

/// Only the Electron one: its uninstaller is "Uninstall Tokkip Healthy PC.exe" (ours is "uninstall.exe").
pub fn old_uninstaller(name: &str, command: &str) -> Option<(String, Vec<String>)> {
    if !name.trim().eq_ignore_ascii_case("Tokkip Healthy PC") || !command.to_lowercase().contains("uninstall tokkip healthy pc.exe") {
        return None;
    }
    let (program, mut args) = crate::tools::software::split_command(command, false);
    let program = program.trim_matches('"').to_string();
    if !args.iter().any(|a| a.eq_ignore_ascii_case("/S")) {
        args.push("/S".into());
    }
    Some((program, args))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn dates() {
        assert_eq!(iso_ms("1970-01-01T00:00:00.000Z"), 0);
        assert_eq!(iso_ms("2026-10-02T09:30:00.000Z"), 1_790_933_400_000);
        assert_eq!(iso_ms("nope"), 0);
    }

    #[test]
    fn brings_the_settings_over() {
        let config = r#"{"settings":{"theme":"dark","language":"es-MX",
            "schedules":[{"enabled":false,"frequency":"daily","day":0,"hour":3},{"enabled":true,"frequency":"weekly","day":0,"hour":21}],
            "gameMode":{"enabledOptimizations":["svc-wsearch","sys-power-plan","mem-clear-standby"],"autoDetect":true,"customGameProcesses":["valorant.exe",""]}}}"#;
        let history = r#"[{"totalSpaceSaved":1000,"timestamp":"2026-10-02T09:30:00.000Z"},{"totalSpaceSaved":0,"timestamp":"2026-11-01T00:00:00Z"},{"totalSpaceSaved":500,"timestamp":"2026-09-01T00:00:00Z"}]"#;
        let p = plan(config, history);
        assert_eq!(p.theme.as_deref(), Some("dark"));
        assert_eq!(p.language.as_deref(), Some("es"));
        assert_eq!(p.routine, Some(Routine { on: true, day: 6, hour: 21 }), "Sunday is our 6");
        let g = p.game.unwrap();
        assert_eq!(g.options, vec!["search", "power"]);
        assert!(g.auto);
        assert_eq!(g.games, vec!["valorant.exe"]);
        assert_eq!((p.freed, p.cleans), (1500, 2));
        assert_eq!(p.last_clean, 1_790_933_400_000);
    }

    #[test]
    fn missing_or_broken_files_bring_nothing() {
        assert_eq!(plan("", "not json"), Plan::default());
        assert_eq!(plan(r#"{"settings":{"theme":"pink","schedule":{"enabled":false}}}"#, "[]"), Plan::default());
    }

    #[test]
    fn finds_only_the_old_uninstaller() {
        let (p, a) = old_uninstaller("Tokkip Healthy PC", r#""C:\Program Files\Tokkip Healthy PC\Uninstall Tokkip Healthy PC.exe" /allusers"#).unwrap();
        assert_eq!(p, r"C:\Program Files\Tokkip Healthy PC\Uninstall Tokkip Healthy PC.exe");
        assert_eq!(a, vec!["/allusers", "/S"]);
        assert!(old_uninstaller("Tokkip Healthy PC", r#""C:\Users\a\AppData\Local\Tokkip Healthy PC\uninstall.exe""#).is_none(), "never this app");
        assert!(old_uninstaller("Otra app", r#""C:\x\Uninstall Tokkip Healthy PC.exe""#).is_none());
    }

    #[test]
    fn done_once() {
        let appdata = tempfile::tempdir().unwrap();
        let data = tempfile::tempdir().unwrap();
        assert!(pending(appdata.path(), data.path()).is_none());
        let old = appdata.path().join("Tokkip Healthy PC");
        std::fs::create_dir_all(&old).unwrap();
        std::fs::write(old.join("config.json"), "{}").unwrap();
        assert_eq!(pending(appdata.path(), data.path()), Some(old));
        std::fs::write(data.path().join(DONE_FILE), "{}").unwrap();
        assert!(pending(appdata.path(), data.path()).is_none());
    }
}
