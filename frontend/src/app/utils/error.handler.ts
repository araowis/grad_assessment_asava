import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';

interface AppError {
  status: number;
  message: string;
  details?: any;
  timestamp?: string;
  context?: string;
}

export function handleError(error: HttpErrorResponse, context: string) {
  console.group(`HTTP Error - ${context}`);
  console.error('Status:', error.status);
  console.error('Status Text:', error.statusText);
  console.error('Message:', error.message);
  console.error('Error Body:', error.error);
  console.error('URL:', error.url);
  console.groupEnd();

  let userMessage = 'An unexpected error occurred. Please try again later.';

  const backendMessage =
    error?.error?.message ||
    error?.error?.error ||
    (typeof error?.error === 'string' ? error.error : null);

  switch (error.status) {
    case 0:
      userMessage =
        'Unable to connect to the server. Please check your network or backend service.';
      break;

    case 400:
      userMessage = backendMessage || 'Invalid request. Please verify your input.';
      break;

    case 401:
      userMessage = 'Authentication failed. Please log in again.';
      break;

    case 403:
      userMessage = 'You do not have permission to perform this action.';
      break;

    case 404:
      userMessage = 'Requested resource was not found.';
      break;

    case 409:
      userMessage = backendMessage || 'Conflict occurred. Resource may already exist.';
      break;

    case 422:
      userMessage = backendMessage || 'Validation failed. Please check your data.';
      break;

    case 500:
      userMessage =
        backendMessage || 'Internal server error. Please try again later.';
      break;

    case 503:
      userMessage =
        'Service is currently unavailable. Please try again after some time.';
      break;

    default:
      userMessage = backendMessage || userMessage;
  }

  const appError: AppError = {
    status: error.status,
    message: userMessage,
    details: error.error,
    timestamp: new Date().toISOString(),
    context,
  };

  return throwError(() => appError);
}