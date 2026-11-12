// What Tokkip says about what this PC has (src-tauri/src/profile.rs): the
// services and privacy advice use it to say why.

import { N_, t } from "../i18n/i18n";

export const NEEDS: Record<string, { has: string; missing: string; name: string }> = {
  printer: { has: N_("Your PC has a printer: leave it on."), missing: N_("I didn't find a printer: you can turn it off."), name: N_("a printer") },
  bluetooth: { has: N_("Your PC has Bluetooth: keep it if you use wireless headphones, mice or controllers."), missing: N_("Your PC has no Bluetooth: you can turn it off."), name: N_("Bluetooth") },
  touch: { has: N_("Your PC has a touch screen or pen: leave it on."), missing: N_("Your PC has no touch screen or pen: you can turn it off."), name: N_("a touch screen") },
  fingerprint: { has: N_("Your PC has a fingerprint reader or Windows Hello camera: keep it."), missing: N_("I didn't find a fingerprint reader or Hello camera: you can turn it off."), name: N_("a fingerprint reader") },
  smartCard: { has: N_("Your PC has a smart card reader: keep it."), missing: N_("I didn't find a smart card reader: you can turn it off."), name: N_("a smart card reader") },
  scanner: { has: N_("I found a scanner or all-in-one: leave it on."), missing: N_("I didn't find a scanner: you can turn it off."), name: N_("a scanner") },
  xbox: { has: N_("You have the Xbox app or an Xbox controller: keep it for your games."), missing: N_("I didn't find the Xbox app or controllers: you can turn it off."), name: N_("Xbox") },
  hyperV: { has: N_("You have Hyper-V, WSL 2 or Sandbox installed: keep it."), missing: N_("You don't have Hyper-V or WSL 2: it does nothing here."), name: N_("Hyper-V") },
  virtualMachine: { has: N_("This PC is a virtual machine: keep it."), missing: N_("This PC isn't a virtual machine: it does nothing here."), name: N_("a virtual machine") },
  wifi: { has: N_("Your PC has Wi-Fi: leave it on."), missing: N_("Your PC has no Wi-Fi: you can turn it off."), name: N_("Wi-Fi") },
  mobileBroadband: { has: N_("Your PC has a SIM or LTE modem: keep it."), missing: N_("Your PC has no SIM: you can turn it off."), name: N_("a SIM") },
  hdd: { has: N_("Windows is on a hard disk: keep it, it helps things open fast."), missing: N_("Windows is on an SSD: turning it off is barely noticed, but gains little."), name: N_("a hard disk") },
  remoteDesktop: { has: N_("Remote Desktop is on: keep it if you connect from elsewhere."), missing: N_("Remote Desktop is off: you can turn it off."), name: N_("Remote Desktop") },
  domain: { has: N_("This PC is on a company domain: keep it."), missing: N_("This PC isn't on a domain: you can turn it off."), name: N_("a company domain") },
  sensors: { has: N_("Your PC has sensors (auto-rotate or brightness): keep it."), missing: N_("I didn't find sensors: you can turn it off."), name: N_("sensors") },
  laptop: { has: N_("It's a laptop: think twice."), missing: N_("It's a desktop."), name: N_("a laptop") },
};

/** What Tokkip says about one need on this PC; "" when he can't tell. */
export function needSays(need: string | null, has: boolean | null): string {
  const n = need ? NEEDS[need] : undefined;
  if (!n || has === null) return "";
  return t(has ? n.has : n.missing);
}

/** "a printer, Bluetooth and Xbox" (or "y" in Spanish). */
export function listOf(items: string[]): string {
  if (items.length < 2) return items.join("");
  return t("{list} and {last}", { list: items.slice(0, -1).join(", "), last: items[items.length - 1] });
}
