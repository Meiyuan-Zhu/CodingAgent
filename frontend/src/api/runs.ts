export interface RunResponse {
  id: string
  status: string
  createdAt: string
  updatedAt: string
  nextSequence: number
  stopReason: string | null
  errorMessage: string | null
}

export interface RunEvent {
  eventId: string
  runId: {
    value: string
  }
  sequence: number
  occurredAt: string
  type: string
  payload: Record<string, unknown>
}

export async function createRun(prompt: string): Promise<RunResponse> {
  const response = await fetch('/api/runs', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ prompt }),
  })

  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }

  return response.json() as Promise<RunResponse>
}

export async function fetchRun(runId: string): Promise<RunResponse> {
  const response = await fetch(`/api/runs/${encodeURIComponent(runId)}`)

  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }

  return response.json() as Promise<RunResponse>
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? `Backend returned HTTP ${response.status}`
  } catch {
    return `Backend returned HTTP ${response.status}`
  }
}
