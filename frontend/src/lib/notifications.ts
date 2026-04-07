import { toast } from 'react-toastify'

export function notifyCreated(entityName: string) {
  toast.success(`${entityName} created successfully.`)
}

export function notifyUpdated(entityName: string) {
  toast.success(`${entityName} updated successfully.`)
}

export function notifyDeleted(entityName: string) {
  toast.success(`${entityName} deleted successfully.`)
}
