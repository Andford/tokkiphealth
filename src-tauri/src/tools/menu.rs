// The right-click menu: what programs added to it (verbs under `shell`, and
// handlers under `shellex\ContextMenuHandlers`), which ones are broken (their
// program or DLL is gone), switching one off and on the way Windows itself
// does (a `LegacyDisable` value for a verb, a "-" before a handler's name), and
// removing broken ones after a .reg backup that can be put back. Following
// Kudu (MIT, Advent Development) and the old Healthy PC: the places it looks,
// how it switches entries off and the Windows entries it never touches.

#![cfg_attr(not(windows), allow(dead_code))]

use serde::Serialize;
use std::path::Path;

/// Where in Explorer the entry shows up, and its key under `Software\Classes`.
pub const SCOPES: [(&str, &str); 6] = [
    ("files", "*"),
    ("folders", "Directory"),
    ("background", r"Directory\Background"),
    ("folder", "Folder"),
    ("drives", "Drive"),
    ("everything", "AllFilesystemObjects"),
];

/// Windows' own verbs: never switched off or removed.
const SAFE_VERBS: [&str; 23] = [
    "open", "edit", "print", "printto", "runas", "opennewwindow", "opennewprocess", "find", "explore", "cmd", "properties", "cut", "copy",
    "paste", "link", "rename", "delete", "sendto", "pintohome", "pintotaskbar", "unpinfromtaskbar", "pintostartscreen", "unpinfromstartscreen",
];

/// Windows' own handlers (Defender, Open with, Send to, zip folders, sharing, libraries…).
const SAFE_CLSIDS: [&str; 13] = [
    "{09a47860-11b0-4da5-afa5-26d86198a780}",
    "{cb3d0f55-bc2c-4c1a-85ed-23ed75b5106b}",
    "{bb64f8a7-bee7-4e1a-ab8d-7d8273f7fdb6}",
    "{09799afb-ad67-11d1-abcd-00c04fc30936}",
    "{7ba4c740-9e81-11cf-99d3-00aa004ae837}",
    "{a470f8cf-a1e8-4f65-8335-227475aa5c46}",
    "{f81e9010-6ea4-11ce-a7ff-00aa003ca9f6}",
    "{888dca60-fc0a-11cf-8f0f-00c04fd7d062}",
    "{f39a0dc0-9cc8-11d0-a599-00c04fd64433}",
    "{e82a2d71-5b2f-43a0-97b8-81be15854de8}",
    "{40dd6e20-7c17-11ce-a804-00aa003ca9f6}",
    "{eccdf543-45cc-11ce-b9bf-0080c87cdba6}",
    "{00021500-0000-0000-c000-000000000046}",
];

#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct MenuEntry {
    pub id: u64,
    /// "verb" (runs a program) or "handler" (a DLL that adds its own items).
    pub kind: &'static str,
    /// What the menu shows, or the key's name.
    pub label: String,
    /// One of SCOPES' first names.
    pub scope: &'static str,
    /// For everyone on the PC (needs administrator rights to change).
    pub machine: bool,
    /// The program or DLL it runs, when it can tell.
    pub target: Option<String>,
    /// Its program or DLL is gone.
    pub broken: bool,
    pub enabled: bool,
    /// Part of Windows: shown, never changed.
    pub protected: bool,
}

/// What the app keeps to change an entry.
#[derive(Debug, Clone, PartialEq)]
pub struct MenuKey {
    pub machine: bool,
    pub verb: bool,
    /// `Software\Classes\<scope>\shell` or `…\shellex\ContextMenuHandlers`.
    pub parent: String,
    /// The subkey's name as it's written now ("-" first for a switched-off handler).
    pub name: String,
    pub enabled: bool,
}

impl MenuKey {
    pub fn key(&self) -> String {
        format!(r"{}\{}", self.parent, self.name)
    }
    pub fn full(&self) -> String {
        format!(r"{}\{}", if self.machine { "HKLM" } else { "HKCU" }, self.key())
    }
    /// A handler's name once switched off or on.
    pub fn handler_name(&self, on: bool) -> String {
        let bare = self.name.trim_start_matches('-');
        if on { bare.to_string() } else { format!("-{bare}") }
    }
}

pub fn is_protected_verb(name: &str) -> bool {
    SAFE_VERBS.contains(&name.trim().to_lowercase().as_str())
}

/// The {CLSID} in a key name or value, in lower case with braces.
pub fn clsid_in(raw: &str) -> Option<String> {
    let start = raw.find('{')?;
    let end = raw[start..].find('}')? + start;
    let inner = &raw[start + 1..end];
    (inner.len() >= 30 && inner.chars().all(|c| c.is_ascii_hexdigit() || c == '-')).then(|| raw[start..=end].to_lowercase())
}

pub fn is_protected_clsid(clsid: &str) -> bool {
    SAFE_CLSIDS.contains(&clsid.to_lowercase().as_str())
}

/// What the menu shows: its own label, unless it's a resource reference ("@shell32.dll,-8506").
pub fn label_of(mui: &str, default: &str, name: &str) -> String {
    let pick = |s: &str| {
        let s = s.trim();
        (!s.is_empty() && !s.starts_with('@')).then(|| s.replace('&', ""))
    };
    pick(mui).or_else(|| pick(default)).unwrap_or_else(|| name.trim_start_matches('-').to_string())
}

/// The DLL an InprocServer32 names, with %VARIABLES% expanded. None unless it's a full path.
pub fn dll_of(raw: &str, env: &dyn Fn(&str) -> Option<String>) -> Option<String> {
    let path = crate::rules::expand(raw.trim().trim_matches('"'), env)?;
    let lower = path.to_lowercase();
    (Path::new(&path).is_absolute() && (lower.ends_with(".dll") || lower.ends_with(".exe"))).then_some(path)
}

#[cfg(windows)]
pub fn scan(keep: &mut dyn FnMut(MenuKey) -> u64) -> Vec<MenuEntry> {
    use super::care::{is_orphan, program_of};
    use winreg::enums::*;
    use winreg::RegKey;
    let env = crate::rules::system_env;
    let mut out = Vec::new();
    let hkcr = RegKey::predef(HKEY_CLASSES_ROOT);
    // A handler's DLL, looked up once per CLSID (64-bit, then 32-bit registration).
    let mut dlls: std::collections::HashMap<String, (Option<String>, String)> = Default::default();
    let mut dll_for = |clsid: &str| -> (Option<String>, String) {
        dlls.entry(clsid.to_string())
            .or_insert_with(|| {
                for base in [r"CLSID", r"WOW6432Node\CLSID"] {
                    if let Ok(k) = hkcr.open_subkey(format!(r"{base}\{clsid}")) {
                        let name: String = k.get_value("").unwrap_or_default();
                        let dll: String = k.open_subkey("InprocServer32").and_then(|s| s.get_value("")).unwrap_or_default();
                        return (dll_of(&dll, &env), name);
                    }
                }
                (None, String::new())
            })
            .clone()
    };
    for machine in [false, true] {
        let root = if machine { RegKey::predef(HKEY_LOCAL_MACHINE) } else { RegKey::predef(HKEY_CURRENT_USER) };
        for (scope, class) in SCOPES {
            // Verbs: <class>\shell\<verb>\command
            let parent = format!(r"Software\Classes\{class}\shell");
            if let Ok(shell) = root.open_subkey(&parent) {
                for name in shell.enum_keys().flatten() {
                    let Ok(k) = shell.open_subkey(&name) else { continue };
                    let command: String = k.open_subkey("command").and_then(|c| c.get_value("")).unwrap_or_default();
                    // Submenus (no command of their own) aren't judged.
                    if command.trim().is_empty() {
                        continue;
                    }
                    let mui: String = k.get_value("MUIVerb").unwrap_or_default();
                    let default: String = k.get_value("").unwrap_or_default();
                    let enabled = k.get_raw_value("LegacyDisable").is_err();
                    let target = program_of(&command, &env);
                    let broken = target.as_deref().is_some_and(is_orphan);
                    let protected = is_protected_verb(&name);
                    let id = keep(MenuKey { machine, verb: true, parent: parent.clone(), name: name.clone(), enabled });
                    out.push(MenuEntry { id, kind: "verb", label: label_of(&mui, &default, &name), scope, machine, target, broken, enabled, protected });
                }
            }
            // Handlers: <class>\shellex\ContextMenuHandlers\<name> = {CLSID}
            let parent = format!(r"Software\Classes\{class}\shellex\ContextMenuHandlers");
            if let Ok(handlers) = root.open_subkey(&parent) {
                for name in handlers.enum_keys().flatten() {
                    let Ok(k) = handlers.open_subkey(&name) else { continue };
                    let value: String = k.get_value("").unwrap_or_default();
                    let Some(clsid) = clsid_in(&name).or_else(|| clsid_in(&value)) else { continue };
                    let (dll, friendly) = dll_for(&clsid);
                    let broken = dll.as_deref().is_some_and(is_orphan);
                    let enabled = !name.starts_with('-');
                    let id = keep(MenuKey { machine, verb: false, parent: parent.clone(), name: name.clone(), enabled });
                    let label = if clsid_in(&name).is_some() { label_of("", &friendly, &clsid) } else { label_of("", &friendly, &name) };
                    out.push(MenuEntry { id, kind: "handler", label, scope, machine, target: dll, broken, enabled, protected: is_protected_clsid(&clsid) });
                }
            }
        }
    }
    out
}

#[cfg(not(windows))]
pub fn scan(_keep: &mut dyn FnMut(MenuKey) -> u64) -> Vec<MenuEntry> {
    Vec::new()
}

/// Switches an entry off or on, the way Windows reads it. Undone by the opposite switch.
#[cfg(windows)]
pub fn set(k: &MenuKey, on: bool) -> Result<(), String> {
    use winreg::enums::*;
    let root = winreg::RegKey::predef(if k.machine { HKEY_LOCAL_MACHINE } else { HKEY_CURRENT_USER });
    if k.verb {
        let key = root.open_subkey_with_flags(k.key(), KEY_ALL_ACCESS).map_err(|e| e.to_string())?;
        return if on {
            match key.delete_value("LegacyDisable") {
                Ok(()) => Ok(()),
                Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(()),
                Err(e) => Err(e.to_string()),
            }
        } else {
            key.set_value("LegacyDisable", &"").map_err(|e| e.to_string())
        };
    }
    let next = k.handler_name(on);
    if next == k.name {
        return Ok(());
    }
    // Copy the key to its new name, then remove the old one (reg.exe has no rename).
    let hive = if k.machine { "HKLM" } else { "HKCU" };
    let from = format!(r"{hive}\{}", k.key());
    let to = format!(r"{hive}\{}\{next}", k.parent);
    let copied = crate::platform::run_hidden("reg.exe", &["copy", &from, &to, "/s", "/f"]).is_some_and(|(ok, _)| ok);
    if !copied {
        return Err("copy".into());
    }
    root.delete_subkey_all(k.key()).map_err(|e| e.to_string())
}

#[cfg(not(windows))]
pub fn set(_k: &MenuKey, _on: bool) -> Result<(), String> {
    Err("windows".into())
}

/// Removes an entry after saving it to `backup` (a .reg file `reg import` can restore).
#[cfg(windows)]
pub fn remove(k: &MenuKey, backup: &Path) -> Result<(), String> {
    use winreg::enums::*;
    let ok = crate::platform::run_hidden("reg.exe", &["export", &k.full(), &backup.display().to_string(), "/y"]).is_some_and(|(ok, _)| ok);
    if !ok {
        return Err("backup".into());
    }
    let root = winreg::RegKey::predef(if k.machine { HKEY_LOCAL_MACHINE } else { HKEY_CURRENT_USER });
    root.delete_subkey_all(k.key()).map_err(|e| e.to_string())
}

#[cfg(not(windows))]
pub fn remove(_k: &MenuKey, _backup: &Path) -> Result<(), String> {
    Err("windows".into())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn windows_verbs_and_handlers_are_kept() {
        assert!(is_protected_verb("Open"));
        assert!(is_protected_verb(" runas "));
        assert!(!is_protected_verb("7-Zip"));
        assert!(is_protected_clsid("{09799AFB-AD67-11D1-ABCD-00C04FC30936}"));
        assert!(!is_protected_clsid("{23170F69-40C1-278A-1000-000100020000}"));
    }

    #[test]
    fn finds_the_clsid() {
        assert_eq!(clsid_in("{23170F69-40C1-278A-1000-000100020000}").as_deref(), Some("{23170f69-40c1-278a-1000-000100020000}"));
        assert_eq!(clsid_in("-{23170F69-40C1-278A-1000-000100020000}").as_deref(), Some("{23170f69-40c1-278a-1000-000100020000}"));
        assert_eq!(clsid_in("7-Zip"), None);
        assert_eq!(clsid_in("{short}"), None);
    }

    #[test]
    fn labels_skip_resource_references() {
        assert_eq!(label_of("@shell32.dll,-8506", "Abrir con &Code", "VSCode"), "Abrir con Code");
        assert_eq!(label_of("", "", "-Dropbox"), "Dropbox");
        assert_eq!(label_of("Git Bash Here", "x", "git_shell"), "Git Bash Here");
    }

    #[test]
    fn handler_names_switch() {
        let k = MenuKey { machine: false, verb: false, parent: "P".into(), name: "7-Zip".into(), enabled: true };
        assert_eq!(k.handler_name(false), "-7-Zip");
        assert_eq!(k.handler_name(true), "7-Zip");
        let off = MenuKey { name: "-7-Zip".into(), ..k };
        assert_eq!(off.handler_name(true), "7-Zip");
        assert_eq!(off.full(), r"HKCU\P\-7-Zip");
    }

    #[test]
    fn dlls_need_a_full_path() {
        let base = if cfg!(windows) { r"C:\PF" } else { "/opt" };
        let env = |n: &str| (n == "ProgramFiles").then(|| base.to_string());
        assert_eq!(dll_of(r#""%ProgramFiles%/7-Zip/7-zip.dll""#, &env), Some(format!("{base}/7-Zip/7-zip.dll")));
        assert_eq!(dll_of("shell32.dll", &env), None);
    }
}
