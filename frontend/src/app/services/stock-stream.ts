
import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { Auth } from './auth';

@Injectable({ providedIn: 'root' })
export class StockStreamService {
  private readonly BASE_URL = 'http://localhost:8088/api/v1/stocks/stream';

  constructor(private zone: NgZone, private authService: Auth) {}

  streamPrice(companyId: string): Observable<number> {
    return new Observable(observer => {
      const token = this.authService.getToken();
      const url = `${this.BASE_URL}/${companyId}?token=${token}`;

      console.log('SSE connecting to:', url);        // add this
      console.log('Token present:', !!token);

      // const es = new EventSource(url);
      const es = new EventSource(url, { withCredentials: true });

      es.onopen = () => console.log('SSE connection opened');

      es.addEventListener('price-update', (event: MessageEvent) => {
        console.log('SSE event received:', event.data); 
        this.zone.run(() => {
          const data = JSON.parse(event.data);
          observer.next(data.price);
        });
      });

      es.onerror = () => {
        // EventSource auto-reconnects — this fires on temporary drops
        console.warn('SSE connection interrupted, auto-reconnecting...');
      };

      // Cleanup when Angular unsubscribes
      return () => es.close();
    });
  }
}