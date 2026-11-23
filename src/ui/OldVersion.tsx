// The old Tokkip Healthy PC (Electron): bring its settings over, once, and
// uninstall it. Shown on Inicio and in Settings while it's still around.

import { useEffect, useState } from "preact/hooks";
import { oldVersion, oldVersionReplace, type OldVersion as Old } from "../core/api";
import { bytes } from "../core/format";
import { loadTheme, saveTheme } from "../core/theme";
import { setLanguage, t, tn } from "../i18n/i18n";
import { Sound } from "../core/sound";
import { Icon } from "./Icon";

export function OldVersion() {
  const [old, setOld] = useState<Old | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState<"" | "all" | "settings">("");
  useEffect(() => {
    void oldVersion().then(setOld).catch(() => setOld(null));
  }, []);
  if (done)
    return (
      <div class="outcome" role="status">
        <Icon name="check" />
        <p>
          {done === "all"
            ? t("Done: your settings are here and the old version is gone. Its data stays in its folder, untouched.")
            : t("Your settings are here. The old version is still installed: Windows asked first, and it wasn't removed.")}
        </p>
      </div>
    );
  if (!old || (!old.data && !old.installed)) return null;

  const p = old.plan;
  const go = async () => {
    setBusy(true);
    try {
      const r = await oldVersionReplace();
      if (r.plan?.theme) saveTheme({ ...loadTheme(), mode: r.plan.theme });
      if (r.plan?.language) setLanguage(r.plan.language);
      setDone(r.uninstalled || !old.installed ? "all" : "settings");
      Sound.play("finish");
    } catch {
      Sound.play("error");
    }
    setBusy(false);
  };
  return (
    <section class="card tool-head">
      <div>
        <h2 class="section-title">{t("The old Tokkip Healthy PC is still here")}</h2>
        <p class="hint">
          {t("This one already does everything it did. Bring your settings over and uninstall it, in one go.")}
          {p && p.freed > 0 && " " + tn("It freed {size} in {n} clean: that's added to what Tokkip has freed with you.", "It freed {size} in {n} cleans: that's added to what Tokkip has freed with you.", p.cleans, { size: bytes(p.freed) })}
          {p?.routine && " " + t("Its scheduled clean becomes the weekly clean.")}
        </p>
      </div>
      <button class="btn filled" disabled={busy} onClick={() => void go()}>
        <Icon name={busy ? "refresh" : "installer"} size={20} />
        {busy ? t("Working…") : old.installed ? t("Bring over and uninstall") : t("Bring my settings over")}
      </button>
    </section>
  );
}
