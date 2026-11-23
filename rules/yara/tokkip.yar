// Tokkip Healthy PC's own YARA rules (MIT, ours). They look for how malware
// behaves, not for one sample: a script that downloads and runs, a shortcut
// that starts a hidden PowerShell, an unsigned program built to inject into
// others. Each finding is shown first and only moved to quarantine if you say
// so. The rule names are a contract with src-tauri/src/tools/scan.rs (RULES).

import "pe"
import "math"

// ── Programs ───────────────────────────────────────────────────────────────

rule tokkip_pe_injector
{
    meta:
        about = "Unsigned program that can write into other programs, and hides it (packed or self-modifying)"
        severity = "high"
    strings:
        $a1 = "CreateRemoteThread" ascii
        $a2 = "VirtualAllocEx" ascii
        $a3 = "WriteProcessMemory" ascii
        $a4 = "NtUnmapViewOfSection" ascii
        $a5 = "ZwUnmapViewOfSection" ascii
        $a6 = "QueueUserAPC" ascii
        $a7 = "SetWindowsHookEx" ascii
        $a8 = "RtlCreateUserThread" ascii
        $a9 = "NtCreateThreadEx" ascii
        $a10 = "MiniDumpWriteDump" ascii
        $a11 = "LsaRetrievePrivateData" ascii
        $a12 = "SamIConnect" ascii
        $a13 = "AmsiScanBuffer" ascii
        $a14 = "EtwEventWrite" ascii
    condition:
        uint16(0) == 0x5A4D and filesize < 32MB
        and pe.number_of_signatures == 0
        and 4 of ($a*)
        and (
            math.entropy(0, filesize) > 7.4
            or for any s in pe.sections : (
                ((s.characteristics & pe.SECTION_MEM_WRITE) != 0) and ((s.characteristics & pe.SECTION_MEM_EXECUTE) != 0)
            )
        )
}

// ── PowerShell ─────────────────────────────────────────────────────────────

rule tokkip_ps_download_run
{
    meta:
        about = "PowerShell that downloads code and runs it straight away"
        severity = "high"
    strings:
        $dl1 = "DownloadString" nocase ascii wide
        $dl2 = "DownloadData" nocase ascii wide
        $dl3 = "Net.WebClient" nocase ascii wide
        $dl4 = "Invoke-WebRequest" nocase ascii wide
        $dl5 = "Invoke-RestMethod" nocase ascii wide
        $run1 = "Invoke-Expression" nocase ascii wide
        $run2 = /\biex[\s(]/ nocase
        $run3 = "[Reflection.Assembly]::Load" nocase ascii wide
    condition:
        filesize < 4MB and any of ($dl*) and any of ($run*)
}

rule tokkip_ps_encoded
{
    meta:
        about = "Hidden PowerShell: a long command written in Base64"
        severity = "high"
    strings:
        $enc = /powershell(\.exe)?[^\r\n]{0,80}\s-(e|en|enc|enco|encodedcommand)\s+[A-Za-z0-9+\/=]{160,}/ nocase
    condition:
        filesize < 4MB and $enc
}

rule tokkip_ps_amsi_bypass
{
    meta:
        about = "Script that switches off Windows' malware checks (AMSI)"
        severity = "high"
    strings:
        $a = "AmsiUtils" nocase ascii wide
        $b = "amsiInitFailed" nocase ascii wide
        $c = "AmsiScanBuffer" nocase ascii wide
        $d = "VirtualProtect" nocase ascii wide
    condition:
        filesize < 4MB and (($a and $b) or ($c and $d))
}

// ── Windows' own tools turned into downloaders ─────────────────────────────

rule tokkip_lolbin_download
{
    meta:
        about = "Script that uses a Windows tool (certutil, bitsadmin, mshta, regsvr32) to fetch something from the internet"
        severity = "medium"
    strings:
        $c1 = /certutil(\.exe)?\s[^\r\n]{0,40}-(urlcache|decode)/ nocase
        $c2 = /bitsadmin(\.exe)?\s[^\r\n]{0,40}\/transfer/ nocase
        $c3 = /mshta(\.exe)?\s+["']?https?:/ nocase
        $c4 = /regsvr32(\.exe)?\s[^\r\n]{0,40}\/i:https?:/ nocase
    condition:
        filesize < 4MB and any of them
}

// ── VBScript, JScript and HTA droppers ─────────────────────────────────────

rule tokkip_script_dropper
{
    meta:
        about = "Script that downloads a file, saves it and runs it"
        severity = "high"
    strings:
        $http1 = "MSXML2.XMLHTTP" nocase ascii wide
        $http2 = "Microsoft.XMLHTTP" nocase ascii wide
        $http3 = "WinHttp.WinHttpRequest" nocase ascii wide
        $http4 = "MSXML2.ServerXMLHTTP" nocase ascii wide
        $save = "ADODB.Stream" nocase ascii wide
        $run1 = "WScript.Shell" nocase ascii wide
        $run2 = "Shell.Application" nocase ascii wide
    condition:
        filesize < 4MB and any of ($http*) and $save and any of ($run*)
}

// ── Shortcuts ──────────────────────────────────────────────────────────────

rule tokkip_lnk_hidden_shell
{
    meta:
        about = "Shortcut that starts a hidden PowerShell or command window"
        severity = "high"
    strings:
        $ps = "powershell" nocase wide ascii
        $cmd = "cmd.exe" nocase wide ascii
        $mshta = "mshta" nocase wide ascii
        $h1 = "-w hidden" nocase wide ascii
        $h2 = "-windowstyle hidden" nocase wide ascii
        $h3 = "-win hidden" nocase wide ascii
        $h4 = "-enc" nocase wide ascii
        $h5 = "http" nocase wide ascii
    condition:
        uint32(0) == 0x0000004C and filesize < 1MB
        and ($ps or $cmd or $mshta)
        and any of ($h*)
}
