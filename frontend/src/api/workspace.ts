export interface WorkspaceFileEntry {
  path: string
  type: 'FILE' | 'DIRECTORY' | 'SYMLINK' | string
  sizeBytes: number
}

export interface WorkspaceFilesResponse {
  root: string
  files: WorkspaceFileEntry[]
  truncated: boolean
}

export interface WorkspaceFileResponse {
  path: string
  content: string
  sizeBytes: number
}

export interface WorkspaceProject {
  id: string
  name: string
  path: string
  createdAt: string
  active: boolean
}

export interface ChooseFolderResponse {
  path: string | null
  cancelled: boolean
}

export async function fetchWorkspaceProjects(): Promise<WorkspaceProject[]> {
  const response = await fetch('/api/workspace/projects')
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<WorkspaceProject[]>
}

export async function addWorkspaceProject(path: string, create: boolean): Promise<WorkspaceProject> {
  const response = await fetch('/api/workspace/projects', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ path, create }),
  })
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<WorkspaceProject>
}

export async function chooseWorkspaceProjectFolder(): Promise<ChooseFolderResponse> {
  const response = await fetch('/api/workspace/projects/choose-folder', {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<ChooseFolderResponse>
}

export async function selectWorkspaceProject(projectId: string): Promise<WorkspaceProject> {
  const response = await fetch(`/api/workspace/projects/${encodeURIComponent(projectId)}/select`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<WorkspaceProject>
}

export async function fetchWorkspaceFiles(path = '.'): Promise<WorkspaceFilesResponse> {
  const response = await fetch(`/api/workspace/files?path=${encodeURIComponent(path)}`)
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<WorkspaceFilesResponse>
}

export async function fetchWorkspaceFile(path: string): Promise<WorkspaceFileResponse> {
  const response = await fetch(`/api/workspace/file?path=${encodeURIComponent(path)}`)
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json() as Promise<WorkspaceFileResponse>
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? `Backend returned HTTP ${response.status}`
  } catch {
    return `Backend returned HTTP ${response.status}`
  }
}
