// Driver updates from Windows Update: what it offers for this PC's devices,
// with Tokkip's word on each (advice.rs), and installing the ones you choose.
// Following Kudu (MIT, Advent Development): its driver manager asks the
// Windows Update Agent (Microsoft.Update.Session) and installs through it, so
// every driver comes signed by Microsoft. Installing needs administrator rights.

use super::advice::{driver_kind, driver_priority};
use serde::Serialize;

#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct DriverUpdate {
    /// Windows Update's id: what is sent back to install it.
    pub id: String,
    pub device: String,
    pub title: String,
    pub class: String,
    pub provider: String,
    pub current_version: String,
    pub current_date: String,
    pub available_date: String,
    pub size: u64,
    /// What it's for ("graphics", "network"…) and how much it's worth ("important"…), from advice.rs.
    pub kind: String,
    pub priority: String,
}

/// Lists the driver updates Windows Update offers, one line each.
pub const SCAN: &str = r#"
$ErrorActionPreference = 'Stop'
$session = New-Object -ComObject Microsoft.Update.Session
$result = $session.CreateUpdateSearcher().Search("IsInstalled=0 AND Type='Driver' AND IsHidden=0")
$installed = @()
try { $installed = @(Get-CimInstance Win32_PnPSignedDriver | Select-Object HardWareID, DriverVersion, DriverDate) } catch {}
foreach ($u in $result.Updates) {
  $hw = "$($u.DriverHardwareID)"
  $cur = $null
  if ($hw) { $cur = $installed | Where-Object { $_.HardWareID -eq $hw } | Select-Object -First 1 }
  $curDate = ''
  if ($cur -and $cur.DriverDate) { try { $curDate = ([datetime]$cur.DriverDate).ToString('yyyy-MM-dd') } catch {} }
  $avail = ''
  if ($u.DriverVerDate) { $avail = $u.DriverVerDate.ToString('yyyy-MM-dd') }
  [pscustomobject]@{
    id = $u.Identity.UpdateID; device = "$($u.DriverModel)"; title = "$($u.Title)"; class = "$($u.DriverClass)"
    provider = "$($u.DriverProvider)"; currentVersion = "$($cur.DriverVersion)"; currentDate = $curDate
    availableDate = $avail; size = [int64]$u.MaxDownloadSize
  } | ConvertTo-Json -Compress
}
"#;

/// Reads SCAN's lines (one JSON object each), with Tokkip's advice.
pub fn parse_scan(out: &str) -> Vec<DriverUpdate> {
    let mut list: Vec<DriverUpdate> = out
        .lines()
        .filter_map(|l| serde_json::from_str::<serde_json::Value>(l.trim()).ok())
        .filter_map(|v| {
            let s = |k: &str| v[k].as_str().unwrap_or("").trim().to_string();
            let id = s("id");
            if !valid_id(&id) {
                return None;
            }
            let class = s("class");
            let kind = driver_kind(&class);
            let (current_date, available_date) = (s("currentDate"), s("availableDate"));
            let title = s("title");
            Some(DriverUpdate {
                device: if s("device").is_empty() { title.clone() } else { s("device") },
                priority: driver_priority(kind, &current_date, &available_date).to_string(),
                kind: kind.to_string(),
                id,
                title,
                class,
                provider: s("provider"),
                current_version: s("currentVersion"),
                current_date,
                available_date,
                size: v["size"].as_u64().unwrap_or(0),
            })
        })
        .collect();
    let rank = |p: &str| match p {
        "important" => 0,
        "recommended" => 1,
        "careful" => 2,
        "optional" => 3,
        _ => 4,
    };
    list.sort_by(|a, b| rank(&a.priority).cmp(&rank(&b.priority)).then_with(|| a.device.cmp(&b.device)));
    list
}

/// Windows Update ids are GUIDs: anything else never reaches the script.
pub fn valid_id(id: &str) -> bool {
    id.len() == 36 && id.chars().all(|c| c.is_ascii_hexdigit() || c == '-')
}

/// Downloads and installs the chosen updates; prints "RESULT|ok|failed|reboot".
pub fn install_script(ids: &[String]) -> Option<String> {
    if ids.is_empty() || !ids.iter().all(|i| valid_id(i)) {
        return None;
    }
    let list = ids.iter().map(|i| format!("'{i}'")).collect::<Vec<_>>().join(",");
    Some(format!(
        r#"
$ErrorActionPreference = 'Stop'
$ids = @({list})
$session = New-Object -ComObject Microsoft.Update.Session
$result = $session.CreateUpdateSearcher().Search("IsInstalled=0 AND Type='Driver'")
$todo = New-Object -ComObject Microsoft.Update.UpdateColl
foreach ($u in $result.Updates) {{ if ($ids -contains $u.Identity.UpdateID) {{ $u.AcceptEula(); $todo.Add($u) | Out-Null }} }}
if ($todo.Count -eq 0) {{ 'RESULT|0|0|False'; return }}
$dl = $session.CreateUpdateDownloader(); $dl.Updates = $todo; $dl.Download() | Out-Null
$inst = $session.CreateUpdateInstaller(); $inst.Updates = $todo; $r = $inst.Install()
$ok = 0; $bad = 0
for ($i = 0; $i -lt $todo.Count; $i++) {{ if ($r.GetUpdateResult($i).ResultCode -eq 2) {{ $ok++ }} else {{ $bad++ }} }}
"RESULT|$ok|$bad|$($r.RebootRequired)"
"#
    ))
}

#[derive(Debug, Clone, Serialize, PartialEq, Default)]
#[serde(rename_all = "camelCase")]
pub struct Installed {
    pub installed: u32,
    pub failed: u32,
    pub reboot: bool,
}

pub fn parse_install(out: &str) -> Option<Installed> {
    let line = out.lines().rev().find(|l| l.trim().starts_with("RESULT|"))?;
    let p: Vec<&str> = line.trim().split('|').collect();
    Some(Installed {
        installed: p.get(1)?.parse().ok()?,
        failed: p.get(2)?.parse().ok()?,
        reboot: p.get(3).is_some_and(|r| r.eq_ignore_ascii_case("true")),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    const GUID: &str = "0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0";

    #[test]
    fn reads_what_windows_update_offers_with_advice() {
        let out = format!(
            "{{\"id\":\"{GUID}\",\"device\":\"Intel Wi-Fi 6\",\"title\":\"Intel - Net - 23.1\",\"class\":\"Net\",\"provider\":\"Intel\",\"currentVersion\":\"22.0\",\"currentDate\":\"2024-01-02\",\"availableDate\":\"2025-03-04\",\"size\":1048576}}\r\n\
             {{\"id\":\"11111111-2222-3333-4444-555555555555\",\"device\":\"\",\"title\":\"Old printer\",\"class\":\"Printer\",\"currentDate\":\"2025-01-01\",\"availableDate\":\"2020-01-01\",\"size\":0}}\r\n\
             {{\"id\":\"rm -rf\",\"title\":\"bad\"}}\r\nnot json\r\n"
        );
        let list = parse_scan(&out);
        assert_eq!(list.len(), 2);
        assert_eq!((list[0].kind.as_str(), list[0].priority.as_str()), ("network", "important"));
        assert_eq!(list[0].size, 1_048_576);
        assert_eq!(list[1].device, "Old printer");
        assert_eq!(list[1].priority, "older");
    }

    #[test]
    fn only_guids_reach_the_install_script() {
        assert!(install_script(&[GUID.to_string()]).unwrap().contains(&format!("'{GUID}'")));
        assert!(install_script(&["x'; Remove-Item C:\\ -Recurse #".to_string()]).is_none());
        assert!(install_script(&[]).is_none());
    }

    #[test]
    fn reads_how_installing_went() {
        let r = parse_install("noise\r\nRESULT|2|1|True\r\n").unwrap();
        assert_eq!(r, Installed { installed: 2, failed: 1, reboot: true });
        assert!(parse_install("nothing").is_none());
    }
}
