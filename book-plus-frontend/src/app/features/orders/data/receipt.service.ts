import { Injectable } from '@angular/core';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import {
  Order,
  ORDER_STATUS_LABEL,
  PAYMENT_METHOD_LABEL,
} from '../models/order.model';

/**
 * Genera una boleta de venta en PDF a partir de un pedido, lista para descargar.
 * Todo se construye en el cliente (jsPDF), sin llamadas extra al backend.
 */
@Injectable({ providedIn: 'root' })
export class ReceiptService {
  download(order: Order): void {
    const doc = new jsPDF({ unit: 'pt', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const margin = 40;
    const indigo: [number, number, number] = [79, 70, 229];
    const ink: [number, number, number] = [30, 41, 59];
    const muted: [number, number, number] = [100, 116, 139];

    const cur = (n: number) =>
      `${order.currency ?? 'PEN'} ${Number(n ?? 0).toFixed(2)}`;
    const shortId = order.orderId.slice(0, 8).toUpperCase();

    // ── Encabezado ────────────────────────────────────────────────────────
    doc.setFillColor(...indigo);
    doc.rect(0, 0, pageW, 70, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(22);
    doc.text('BookPlus', margin, 44);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text('Librería digital y física', margin, 58);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.text('BOLETA DE VENTA ELECTRÓNICA', pageW - margin, 40, { align: 'right' });
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text(`N.º B001-${shortId}`, pageW - margin, 56, { align: 'right' });

    // ── Datos del pedido ──────────────────────────────────────────────────
    let y = 100;
    doc.setTextColor(...ink);
    doc.setFontSize(10);

    const created = new Date(order.createdAt);
    const fecha = isNaN(created.getTime())
      ? '—'
      : created.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });

    const leftLines = [
      ['Pedido', `#${shortId}`],
      ['Fecha', fecha],
      ['Estado', ORDER_STATUS_LABEL[order.status]],
      ['Entrega', order.deliveryType === 'DIGITAL' ? 'Digital (descarga)' : 'Física (envío)'],
      ['Pago', order.paymentMethod ? PAYMENT_METHOD_LABEL[order.paymentMethod] : '—'],
    ];
    leftLines.forEach(([k, v], i) => {
      doc.setTextColor(...muted);
      doc.text(`${k}:`, margin, y + i * 15);
      doc.setTextColor(...ink);
      doc.text(String(v), margin + 70, y + i * 15);
    });

    // Dirección de envío (si es física)
    if (order.deliveryType !== 'DIGITAL' && order.shippingAddress) {
      const a = order.shippingAddress;
      const rx = pageW / 2 + 20;
      doc.setTextColor(...muted);
      doc.text('Enviar a:', rx, y);
      doc.setTextColor(...ink);
      const addr = [
        a.recipientName,
        a.street,
        `${a.city}, ${a.state} ${a.postalCode}`,
        a.country,
      ];
      addr.forEach((line, i) => doc.text(String(line ?? ''), rx, y + 15 + i * 14));
    }

    y += leftLines.length * 15 + 20;

    // ── Tabla de ítems ────────────────────────────────────────────────────
    autoTable(doc, {
      startY: y,
      head: [['Descripción', 'Cant.', 'P. Unit.', 'Importe']],
      body: order.items.map((it) => [
        it.title,
        String(it.quantity),
        cur(it.unitPrice),
        cur(it.subtotal ?? it.unitPrice * it.quantity),
      ]),
      theme: 'striped',
      headStyles: { fillColor: indigo, halign: 'left' },
      columnStyles: {
        1: { halign: 'center', cellWidth: 50 },
        2: { halign: 'right', cellWidth: 80 },
        3: { halign: 'right', cellWidth: 90 },
      },
      styles: { fontSize: 9, cellPadding: 6 },
      margin: { left: margin, right: margin },
    });

    // ── Totales ───────────────────────────────────────────────────────────
    const gross = order.items.reduce(
      (s, it) => s + (it.subtotal ?? it.unitPrice * it.quantity),
      0,
    );
    const discount = order.discountAmount ?? 0;
    // @ts-expect-error lastAutoTable lo añade el plugin jspdf-autotable
    let ty = (doc.lastAutoTable?.finalY ?? y) + 24;
    const rightX = pageW - margin;
    const labelX = pageW - margin - 160;

    const totalRow = (label: string, value: string, bold = false) => {
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      doc.setFontSize(bold ? 12 : 10);
      doc.setTextColor(...(bold ? ink : muted));
      doc.text(label, labelX, ty);
      doc.setTextColor(...ink);
      doc.text(value, rightX, ty, { align: 'right' });
      ty += bold ? 20 : 16;
    };

    totalRow('Subtotal', cur(gross));
    if (discount > 0) {
      totalRow(`Descuento${order.couponCode ? ' (' + order.couponCode + ')' : ''}`, `- ${cur(discount)}`);
    }
    totalRow('TOTAL', cur(order.total), true);

    // ── Pie ───────────────────────────────────────────────────────────────
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(...muted);
    const footY = doc.internal.pageSize.getHeight() - 40;
    doc.text(
      'Documento generado electrónicamente por BookPlus. Gracias por tu compra.',
      margin,
      footY,
    );
    if (order.paymentId) {
      doc.text(`Ref. de pago: ${order.paymentId}`, margin, footY + 12);
    }

    doc.save(`boleta-${shortId}.pdf`);
  }
}
