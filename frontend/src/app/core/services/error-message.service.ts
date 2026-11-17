import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ErrorMessageService {
  getHttpErrorMessage(error: unknown, fallback?: string): string {
    const payload = error instanceof HttpErrorResponse
      ? error.error
      : isRecord(error) && 'error' in error
        ? error['error']
        : error;
    const message = this.getMessage(payload);
    const details = this.getDetails(payload);
    const backendMessage = [message, details]
      .filter((value, index, values) => value && values.indexOf(value) === index)
      .join(': ');

    return backendMessage || fallback || this.getStatusMessage(this.getStatus(error));
  }

  private getMessage(payload: unknown): string {
    if (typeof payload === 'string') {
      return payload.trim();
    }
    if (!isRecord(payload)) {
      return '';
    }

    for (const key of ['message', 'detail', 'title', 'error']) {
      const value = payload[key];
      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }
    return '';
  }

  private getDetails(payload: unknown): string {
    if (!isRecord(payload)) {
      return '';
    }

    const details = payload['details'];
    if (typeof details === 'string') {
      return details.trim();
    }
    if (Array.isArray(details)) {
      return details
        .map((detail) => typeof detail === 'string' ? detail.trim() : '')
        .filter(Boolean)
        .join('; ');
    }
    if (isRecord(details)) {
      return Object.entries(details)
        .map(([field, detail]) => {
          if (typeof detail === 'string' && detail.trim()) {
            return `${field}: ${detail.trim()}`;
          }
          if (Array.isArray(detail)) {
            const messages = detail.filter(
              (value): value is string => typeof value === 'string' && Boolean(value.trim()),
            );
            return messages.length ? `${field}: ${messages.join(', ')}` : '';
          }
          return '';
        })
        .filter(Boolean)
        .join('; ');
    }
    return '';
  }

  private getStatus(error: unknown): number {
    if (error instanceof HttpErrorResponse) {
      return error.status;
    }
    return isRecord(error) && typeof error['status'] === 'number'
      ? error['status']
      : 0;
  }

  private getStatusMessage(status: number): string {
    const messages: Record<number, string> = {
      0: 'Unable to reach the server. Check your connection and try again.',
      400: 'Please check the information you entered.',
      401: 'Your session has expired. Please sign in again.',
      403: 'You do not have permission to perform this action.',
      404: 'The requested resource could not be found.',
      408: 'The request timed out. Please try again.',
      409: 'This request conflicts with existing data.',
      413: 'The submitted data is too large.',
      415: 'The submitted data format is not supported.',
      422: 'Please check the information you entered.',
      423: 'This account or resource is currently locked.',
      429: 'Too many requests. Please wait and try again.',
      500: 'The server encountered an error. Please try again later.',
      502: 'The server is temporarily unavailable. Please try again later.',
      503: 'The server is temporarily unavailable. Please try again later.',
      504: 'The server took too long to respond. Please try again.',
    };

    return messages[status] ?? 'Something went wrong. Please try again.';
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
