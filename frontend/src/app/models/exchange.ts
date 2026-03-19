export type OrderType = 'BUY' | 'SELL';
export type OrderStatus = 'OPEN' | 'MATCHED' | 'CANCELLED';

export interface OrderRequest {
  userId: number;
  companyId: string;
  quantity: number;
  price: number;
  type: OrderType;
}

export interface OrderResponse {
  id: number;
  userId: number;
  companyId: string;
  quantity: number;
  price: number;
  type: OrderType;
  status: OrderStatus;
  createdAt: string;
}

export interface TradeResponse {
  id: number;
  buyerId: number;
  sellerId: number;
  companyId: string;
  quantity: number;
  price: number;
  executedAt: string;
}