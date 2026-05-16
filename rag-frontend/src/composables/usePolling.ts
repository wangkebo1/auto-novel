import { onUnmounted, ref } from 'vue'

export function usePolling(task: () => void | Promise<void>, intervalMs = 4000) {
  const running = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  const stop = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    running.value = false
  }

  const start = () => {
    stop()
    running.value = true
    timer = setInterval(() => {
      void task()
    }, intervalMs)
  }

  onUnmounted(() => {
    stop()
  })

  return {
    running,
    start,
    stop,
  }
}
