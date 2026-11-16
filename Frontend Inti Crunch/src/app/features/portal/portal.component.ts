import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth.service';
import { DataRow, errorMessage, labels, money } from '../../core/ui';
import { CartLine, CatalogProduct, CheckoutComponent } from './checkout.component';
import { ReservationComponent } from './reservation.component';

const CART_KEY = 'inti-crunch-cart';

@Component({ selector: 'ic-portal', standalone: true, imports: [CommonModule, RouterLink, CheckoutComponent, ReservationComponent], templateUrl: './portal.component.html' })
export class PortalComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly detector = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  readonly money = money;
  readonly labels = labels;
  readonly fallbackImage = 'https://images.unsplash.com/photo-1626645738196-c2a7c87a8f58?auto=format&fit=crop&w=900&q=85';
  products: CatalogProduct[] = []; promotions: DataRow[] = [];
  category = 'Todo'; loading = true; error = '';
  cart: CartLine[] = []; cartOpen = false; checkout = false; reservationOpen = false; resumeToken = '';
  lastToken = '';
  get categories(): string[] { return ['Todo', ...new Set(this.products.map(product => product.category))]; }
  get filteredProducts(): CatalogProduct[] { return this.category === 'Todo' ? this.products : this.products.filter(product => product.category === this.category); }
  get count(): number { return this.cart.reduce((total, line) => total + line.quantity, 0); }
  get cartTotal(): number { return this.cart.reduce((total, line) => total + line.product.price * line.quantity, 0); }

  async ngOnInit(): Promise<void> {
    try { this.lastToken = localStorage.getItem('inti-crunch-last-order') ?? ''; const saved = sessionStorage.getItem(CART_KEY); if (saved) this.cart = JSON.parse(saved); } catch { /* sin almacenamiento */ }
    await this.load();
  }
  async load(): Promise<void> {
    this.loading = true; this.error = '';
    try { [this.products, this.promotions] = await Promise.all([firstValueFrom(this.api.get<CatalogProduct[]>('/public/catalog/products')), firstValueFrom(this.api.get<DataRow[]>('/public/promotions'))]); }
    catch (error) { this.error = errorMessage(error); } finally { this.loading = false; this.detector.markForCheck(); }
  }
  scroll(id: string): void { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }); }
  private saveCart(): void { try { sessionStorage.setItem(CART_KEY, JSON.stringify(this.cart)); } catch { /* sin almacenamiento */ } }
  add(product: CatalogProduct): void { const line = this.cart.find(item => item.product.id === product.id && !item.optionIds.length && !item.notes); if (line) line.quantity++; else this.cart.push({ product, quantity: 1, optionIds: [], notes: '' }); this.cartOpen = true; this.saveCart(); }
  change(line: CartLine, amount: number): void { line.quantity += amount; if (line.quantity <= 0) this.cart.splice(this.cart.indexOf(line), 1); this.saveCart(); }
  quantityOf(product: CatalogProduct): number { return this.cart.filter(line => line.product.id === product.id).reduce((sum, line) => sum + line.quantity, 0); }
  startCheckout(): void { this.cartOpen = false; this.auth.require(() => { this.resumeToken = ''; this.checkout = true; this.detector.markForCheck(); }); }
  startReservation(): void { this.auth.require(() => { this.reservationOpen = true; this.detector.markForCheck(); }); }
  placed(token: string): void { this.lastToken = token; this.cart = []; this.saveCart(); try { localStorage.setItem('inti-crunch-last-order', token); } catch { /* sin almacenamiento */ } }
  track(): void { this.resumeToken = this.lastToken; this.checkout = true; }
  closeCheckout(): void { this.checkout = false; this.resumeToken = ''; this.saveCart(); }
  promoText(promotion: DataRow): string { return promotion['discountPercent'] ? promotion['discountPercent'] + ' % de descuento' : money(promotion['discountAmount']) + ' de descuento'; }
}
