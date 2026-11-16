import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
@Injectable({providedIn:'root'})
export class ApiService {
  private readonly http=inject(HttpClient);
  private readonly base='http://localhost:8080/api/v1';
  private url(path:string):string{return path.startsWith('http')?path:`${this.base}${path.startsWith('/')?'':'/'}${path}`;}
  get<T>(path:string){return this.http.get<T>(this.url(path)).pipe(timeout(15000));}
  post<T>(path:string,body:unknown){return this.request<T>('POST',path,body);}
  patch<T>(path:string,body:unknown={}){return this.request<T>('PATCH',path,body);}
  request<T>(method:'POST'|'PATCH'|'PUT'|'DELETE',path:string,body:unknown={}){return this.http.request<T>(method,this.url(path),{body}).pipe(timeout(30000));}
  pdf(path:string){return this.http.get(this.url(path),{responseType:'blob'}).pipe(timeout(30000));}
}
