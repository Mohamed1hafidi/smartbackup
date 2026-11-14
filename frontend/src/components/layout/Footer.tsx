import { APP_VERSION } from '../../api/config'

export function Footer() {
  return (
    <footer className="mt-auto border-t border-slate-200 px-4 py-5 sm:px-6 lg:px-8">
      <div className="flex flex-col items-center justify-between gap-2 text-xs text-slate-400 sm:flex-row">
        <p>© {new Date().getFullYear()} SmartBackup. Tous droits réservés.</p>
        <p>Version {APP_VERSION} · Sauvegardes chiffrées</p>
      </div>
    </footer>
  )
}
