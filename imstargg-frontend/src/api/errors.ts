export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly path: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export class NotFoundError extends ApiError {
  constructor(path: string) {
    super(404, path, `Not found: ${path}`);
    this.name = "NotFoundError";
  }
}

class ServerError extends ApiError {
  constructor(status: number, path: string) {
    super(status, path, `Server error ${status}: ${path}`);
    this.name = "ServerError";
  }
}

export class NetworkError extends ApiError {
  constructor(path: string, cause?: unknown) {
    super(0, path, `Network error: ${path}`);
    this.name = "NetworkError";
    if (cause) (this as { cause?: unknown }).cause = cause;
  }
}

export async function toApiError(res: Response, path: string): Promise<ApiError> {
  if (res.status === 404) return new NotFoundError(path);
  if (res.status >= 500) return new ServerError(res.status, path);
  let detail = "";
  try {
    detail = await res.text();
  } catch {
    // ignore
  }
  return new ApiError(res.status, path, detail || `HTTP ${res.status}`);
}
