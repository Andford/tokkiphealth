// What this PC has (printers, Bluetooth, a pen, Xbox, Hyper-V, a battery, a
// hard disk…), so Tokkip's advice fits it: the Print Spooler is only worth
// keeping with a printer. Asked of Windows once per run; nothing leaves the PC.

use serde::Serialize;
use std::sync::Mutex;

#[derive(Debug, Clone, Default, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PcProfile {
    /// Real printers (not "Print to PDF", XPS, OneNote or Fax).
    pub printers: u32,
    pub bluetooth: bool,
    /// A touch screen or a pen.
    pub touch: bool,
    pub fingerprint: bool,
    pub smart_card: bool,
    pub scanner: bool,
    /// The Xbox app, Gaming Services or an Xbox controller.
    pub xbox: bool,
    /// Hyper-V, WSL 2 or Windows Sandbox.
    pub hyper_v: bool,
    /// This PC is itself a virtual machine.
    pub virtual_machine: bool,
    /// It has a battery.
    pub laptop: bool,
    pub wifi: bool,
    pub mobile_broadband: bool,
    /// Windows lives on a spinning hard disk.
    pub hdd: bool,
    pub remote_desktop: bool,
    pub domain: bool,
    /// Light, motion or orientation sensors.
    pub sensors: bool,
}

impl PcProfile {
    /// Whether the PC has what a service or a setting is for (see rules/*.json `needs`).
    pub fn has(&self, need: &str) -> bool {
        match need {
            "printer" => self.printers > 0,
            "bluetooth" => self.bluetooth,
            "touch" => self.touch,
            "fingerprint" => self.fingerprint,
            "smartCard" => self.smart_card,
            "scanner" => self.scanner,
            "xbox" => self.xbox,
            "hyperV" => self.hyper_v,
            "virtualMachine" => self.virtual_machine,
            "laptop" => self.laptop,
            "wifi" => self.wifi,
            "mobileBroadband" => self.mobile_broadband,
            "hdd" => self.hdd,
            "remoteDesktop" => self.remote_desktop,
            "domain" => self.domain,
            "sensors" => self.sensors,
            _ => true,
        }
    }
}

/// Reads what the script printed; anything missing counts as "not there".
pub fn parse(json: &str) -> Option<PcProfile> {
    let v: serde_json::Value = serde_json::from_str(json.trim()).ok()?;
    let o = v.as_object()?;
    let flag = |k: &str| o.get(k).and_then(|x| x.as_bool()).unwrap_or(false);
    Some(PcProfile {
        printers: o.get("printers").and_then(|x| x.as_u64()).unwrap_or(0).min(999) as u32,
        bluetooth: flag("bluetooth"),
        touch: flag("touch"),
        fingerprint: flag("fingerprint"),
        smart_card: flag("smartCard"),
        scanner: flag("scanner"),
        xbox: flag("xbox"),
        hyper_v: flag("hyperV"),
        virtual_machine: flag("virtualMachine"),
        laptop: flag("laptop"),
        wifi: flag("wifi"),
        mobile_broadband: flag("mobileBroadband"),
        hdd: flag("hdd"),
        remote_desktop: flag("remoteDesktop"),
        domain: flag("domain"),
        sensors: flag("sensors"),
    })
}

const SCRIPT: &str = r#"
$ErrorActionPreference = 'SilentlyContinue'
function Has($cls) { [bool](Get-PnpDevice -PresentOnly -Class $cls -Status OK -ErrorAction SilentlyContinue) }
$virtual = 'Microsoft Print to PDF|Microsoft XPS|OneNote|Fax|Send To|PDF|Snagit'
$printers = @(Get-CimInstance Win32_Printer | Where-Object { $_.Name -notmatch $virtual -and $_.PortName -notmatch '^(PORTPROMPT|nul|FILE|SHRFAX|XPS|PDF)' }).Count
$hid = @(Get-PnpDevice -PresentOnly -Class HIDClass -ErrorAction SilentlyContinue)
$touch = [bool]($hid | Where-Object { $_.FriendlyName -match 'touch ?screen|digitizer|pen|t.ctil' })
$cs = Get-CimInstance Win32_ComputerSystem
$vm = [bool]("$($cs.Manufacturer) $($cs.Model)" -match 'Virtual|VMware|VirtualBox|KVM|QEMU|Xen|Parallels')
$nets = @(Get-NetAdapter -Physical -ErrorAction SilentlyContinue)
$wifi = [bool]($nets | Where-Object { $_.NdisPhysicalMedium -eq 9 -or $_.InterfaceDescription -match 'Wi-?Fi|Wireless|802\.11|WLAN' })
$wwan = [bool]($nets | Where-Object { $_.NdisPhysicalMedium -eq 8 -or $_.InterfaceDescription -match 'Mobile Broadband|WWAN|LTE|5G' })
$hdd = $false
try {
  $disk = Get-Partition -DriveLetter $env:SystemDrive.Substring(0, 1) | Get-Disk
  $media = (Get-PhysicalDisk | Where-Object { $_.DeviceId -eq "$($disk.Number)" }).MediaType
  $hdd = "$media" -eq 'HDD'
} catch {}
$ts = Get-ItemProperty 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -ErrorAction SilentlyContinue
$xbox = [bool](Get-AppxPackage -Name Microsoft.GamingApp) -or [bool](Get-AppxPackage -Name Microsoft.GamingServices) -or [bool](Get-AppxPackage -Name Microsoft.XboxApp) -or [bool](Get-PnpDevice -PresentOnly -ErrorAction SilentlyContinue | Where-Object { $_.FriendlyName -match 'Xbox' })
[pscustomobject]@{
  printers = $printers; bluetooth = Has 'Bluetooth'; touch = $touch; fingerprint = Has 'Biometric'
  smartCard = Has 'SmartCardReader'; scanner = Has 'Image'; xbox = $xbox
  hyperV = [bool](Get-Service vmcompute -ErrorAction SilentlyContinue); virtualMachine = $vm
  laptop = [bool](Get-CimInstance Win32_Battery); wifi = $wifi; mobileBroadband = $wwan; hdd = $hdd
  remoteDesktop = ($ts -and $ts.fDenyTSConnections -eq 0); domain = [bool]$cs.PartOfDomain; sensors = Has 'Sensor'
} | ConvertTo-Json -Compress
"#;

static CACHE: Mutex<Option<PcProfile>> = Mutex::new(None);

/// What this PC has, or None when Windows couldn't say (and off Windows).
/// Slow the first time (a few seconds), then kept for the run.
pub fn get() -> Option<PcProfile> {
    if let Some(p) = CACHE.lock().unwrap_or_else(|e| e.into_inner()).clone() {
        return Some(p);
    }
    let p = crate::platform::powershell(SCRIPT).and_then(|out| parse(&out))?;
    *CACHE.lock().unwrap_or_else(|e| e.into_inner()) = Some(p.clone());
    Some(p)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reads_what_windows_said_and_the_rest_is_missing() {
        let p = parse("{\"printers\":2,\"bluetooth\":true,\"touch\":\"yes\"}\r\n").unwrap();
        assert_eq!(p.printers, 2);
        assert!(p.bluetooth && !p.touch && !p.xbox);
        assert!(p.has("printer") && !p.has("xbox"));
        assert!(parse("").is_none() && parse("Get-PnpDevice : error").is_none());
    }
}
