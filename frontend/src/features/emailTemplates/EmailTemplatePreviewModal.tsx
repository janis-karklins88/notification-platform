import type { EmailTemplate } from './types'

type EmailTemplatePreviewModalProps = {
  open: boolean
  template: EmailTemplate | null
  onClose: () => void
}

export function EmailTemplatePreviewModal({
  open,
  template,
  onClose,
}: EmailTemplatePreviewModalProps) {
  if (!open || !template) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="flex max-h-[calc(100vh-3rem)] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Template preview</h2>
          <p className="mt-1 text-sm text-slate-600">
            Quick visual preview of the email.
          </p>
        </div>

        <div className="flex-1 overflow-y-auto bg-slate-100 px-6 py-6">
          <div className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-200 px-6 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
                Subject
              </p>
              <p className="mt-2 text-base font-medium text-slate-900">{template.subject}</p>
            </div>

            <div className="overflow-hidden">
              {template.html ? (
                <iframe
                  className="h-144 w-full bg-white"
                  srcDoc={template.body}
                  title={`Preview for ${template.name}`}
                />
              ) : (
                <div className="h-144 overflow-auto px-6 py-5 text-sm leading-7 text-slate-900 whitespace-pre-wrap">
                  {template.body}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="border-t border-slate-200 px-6 py-4">
          <div className="flex justify-end">
            <button
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
              onClick={onClose}
              type="button"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
