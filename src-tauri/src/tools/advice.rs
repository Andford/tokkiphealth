// Tokkip's advice, fitted to this PC (profile.rs): whether to switch a Windows
// service off, how freely to protect a privacy setting, and how much a driver
// update is worth. The rules say how risky something is (rules/services.json,
// rules/privacy.json); the PC's profile says whether this PC uses it at all.

use crate::profile::PcProfile;
use serde::Serialize;

/// What Tokkip says about a service.
#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ServiceAdvice {
    /// "off" (switch it off without fear), "optional" (your call), "keep"
    /// (this PC uses it) or "never" (Windows needs it).
    pub verdict: String,
    /// What it hinges on, when it depends on the PC ("printer", "bluetooth"…).
    pub need: Option<String>,
    /// Whether this PC has it (None without a profile).
    pub has: Option<bool>,
}

pub fn advise_service(safety: &str, needs: Option<&str>, soft_without: bool, profile: Option<&PcProfile>) -> ServiceAdvice {
    let verdict = |v: &str| v.to_string();
    if safety == "unsafe" {
        return ServiceAdvice { verdict: verdict("never"), need: None, has: None };
    }
    let plain = if safety == "safe" { "off" } else { "optional" };
    match (needs, profile) {
        (Some(n), Some(p)) => {
            let has = p.has(n);
            let v = if has {
                "keep"
            } else if soft_without {
                "optional"
            } else {
                "off"
            };
            ServiceAdvice { verdict: verdict(v), need: Some(n.to_string()), has: Some(has) }
        }
        // Without the PC's profile, a service that depends on it is never "off" unless it's safe anyway.
        (Some(n), None) => ServiceAdvice { verdict: verdict(plain), need: Some(n.to_string()), has: None },
        (None, _) => ServiceAdvice { verdict: verdict(plain), need: None, has: None },
    }
}

/// How freely to protect a privacy setting: "recommended", "caution" or
/// "advanced". With what `needs` names, it goes one step up (handwriting
/// data on a PC with a pen, Find My Device on a laptop).
pub fn privacy_level(level: &str, needs: Option<&str>, profile: Option<&PcProfile>) -> (String, Option<String>) {
    match (needs, profile) {
        (Some(n), Some(p)) if p.has(n) => {
            let up = match level {
                "recommended" => "caution",
                _ => "advanced",
            };
            (up.to_string(), Some(n.to_string()))
        }
        _ => (level.to_string(), None),
    }
}

/// What a driver is for (Windows Update's driver class).
pub fn driver_kind(class: &str) -> &'static str {
    let c = class.trim().to_lowercase();
    let is = |names: &[&str]| names.iter().any(|n| c == *n);
    if is(&["display", "video"]) {
        "graphics"
    } else if is(&["net", "netclient", "netservice", "nettrans"]) {
        "network"
    } else if is(&["media", "audioendpoint", "audioprocessingobject", "sound"]) {
        "audio"
    } else if c == "bluetooth" {
        "bluetooth"
    } else if is(&["system", "computer", "processor"]) {
        "chipset"
    } else if c.starts_with("usb") {
        "usb"
    } else if ["scsiadapter", "hdc", "diskdrive", "storage", "volume", "nvme"].iter().any(|n| c.starts_with(n)) {
        "storage"
    } else if c == "firmware" {
        "firmware"
    } else if is(&["camera", "image"]) {
        "camera"
    } else if is(&["printer", "printqueue"]) {
        "printer"
    } else if is(&["hidclass", "keyboard", "mouse", "biometric", "sensor", "monitor"]) {
        "input"
    } else {
        "other"
    }
}

/// How much a driver update is worth: "important", "recommended", "optional",
/// "careful" (worth it, but with a restore point and no interruptions) or
/// "older" (older than the one installed: skip it). Dates are YYYY-MM-DD.
pub fn driver_priority(kind: &str, current_date: &str, available_date: &str) -> &'static str {
    let day = |d: &str| d.len() == 10 && d.as_bytes()[4] == b'-' && d.as_bytes()[7] == b'-';
    if day(current_date) && day(available_date) && available_date < current_date {
        return "older";
    }
    match kind {
        "network" | "chipset" => "important",
        "graphics" | "audio" | "bluetooth" | "usb" => "recommended",
        "storage" | "firmware" => "careful",
        _ => "optional",
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn bare() -> PcProfile {
        PcProfile::default()
    }

    #[test]
    fn never_lets_you_switch_off_what_windows_needs() {
        assert_eq!(advise_service("unsafe", None, false, Some(&bare())).verdict, "never");
    }

    #[test]
    fn keeps_the_print_spooler_only_with_a_printer() {
        let printer = PcProfile { printers: 1, ..bare() };
        let a = advise_service("caution", Some("printer"), false, Some(&printer));
        assert_eq!((a.verdict.as_str(), a.has), ("keep", Some(true)));
        assert_eq!(advise_service("caution", Some("printer"), false, Some(&bare())).verdict, "off");
        // Without seeing the PC, it stays your call.
        assert_eq!(advise_service("caution", Some("printer"), false, None).verdict, "optional");
    }

    #[test]
    fn sysmain_is_only_your_call_on_an_ssd() {
        assert_eq!(advise_service("caution", Some("hdd"), true, Some(&bare())).verdict, "optional");
        assert_eq!(advise_service("caution", Some("hdd"), true, Some(&PcProfile { hdd: true, ..bare() })).verdict, "keep");
    }

    #[test]
    fn privacy_goes_up_a_step_with_a_pen_or_a_laptop() {
        let pen = PcProfile { touch: true, ..bare() };
        assert_eq!(privacy_level("recommended", Some("touch"), Some(&pen)), ("caution".into(), Some("touch".into())));
        assert_eq!(privacy_level("recommended", Some("touch"), Some(&bare())), ("recommended".into(), None));
        let laptop = PcProfile { laptop: true, ..bare() };
        assert_eq!(privacy_level("caution", Some("laptop"), Some(&laptop)).0, "advanced");
    }

    #[test]
    fn sorts_drivers_and_skips_older_ones() {
        assert_eq!(driver_kind("Display"), "graphics");
        assert_eq!(driver_kind("MEDIA"), "audio");
        assert_eq!(driver_kind("SCSIAdapter"), "storage");
        assert_eq!(driver_kind("Something"), "other");
        assert_eq!(driver_priority("network", "2025-03-01", "2024-11-10"), "older");
        assert_eq!(driver_priority("network", "", "2025-01-01"), "important");
        assert_eq!(driver_priority("firmware", "2024-01-01", "2025-01-01"), "careful");
    }
}
