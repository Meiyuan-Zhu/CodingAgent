export interface HealthResponse {
  status: string
  service: string
  javaVersion: number
  serverTime: string
}

export async function fetchHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/health')

  if (!response.ok) {
    throw new Error(`Backend returned HTTP ${response.status}`)
  }

  return response.json() as Promise<HealthResponse>
}
