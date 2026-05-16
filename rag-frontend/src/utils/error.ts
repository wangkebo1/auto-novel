import { ElMessage } from 'element-plus'

export function isCancelError(error: unknown): boolean {
  const e = error as { code?: string; message?: string }
  return e?.code === 'ERR_CANCELED' || e?.message === 'cancel' || e?.message === 'canceled'
}

export function handleViewError(
  scope: string,
  error: unknown,
  fallbackMessage = '操作失败',
  notify = true,
): void {
  const e = error as { response?: { data?: { message?: string } }; message?: string }
  const message = e?.response?.data?.message || e?.message || fallbackMessage

  console.error(`[${scope}]`, error)
  if (notify) {
    ElMessage.error(message)
  }
}
