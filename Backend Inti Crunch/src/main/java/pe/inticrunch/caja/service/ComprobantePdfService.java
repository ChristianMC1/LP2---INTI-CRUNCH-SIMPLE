package pe.inticrunch.caja.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.awt.Color;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;

@Service
public class ComprobantePdfService {
    public byte[] render(Map<String,Object> receipt,List<Map<String,Object>> lines){
        try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            Writer writer=new Writer(document,receipt);
            writer.page();
            writer.paragraph("CLIENTE",true);
            writer.paragraph(Objects.toString(receipt.get("customer_name"),"Consumidor final"),false);
            writer.paragraph(Objects.toString(receipt.get("customer_document_type"),"Documento")+": "+Objects.toString(receipt.get("customer_document_number"),"No consignado")+" | Teléfono: "+Objects.toString(receipt.get("customer_phone"),"-"),false);
            writer.paragraph("Dirección: "+Objects.toString(receipt.get("customer_address"),"No consignada"),false);
            writer.paragraph("Pedido N° "+Objects.toString(receipt.get("order_number"),Objects.toString(receipt.get("order_id"),""))+" | Cajero: "+Objects.toString(receipt.get("cashier"),"Equipo Inti Crunch"),false);
            writer.paragraph("Pago: "+Objects.toString(receipt.get("payment_summary"),"Simulado"),false);
            writer.cursor-=12;
            writer.tableHeader();
            for(var line:lines){
                // Cada fila reserva su propio alto: el texto nunca toca la línea divisoria.
                List<String> name=writer.wrap(Objects.toString(line.get("product_name"),"Producto"),300,10);
                float height=10+name.size()*14+8;
                writer.rowSpace(Math.min(height,220));
                float top=writer.cursor;
                float baseline=top-14;
                writer.text(49,baseline,Objects.toString(line.get("quantity"),"1"),10,false);
                writer.right(459,baseline,amount(line.get("unit_price")),10,false);
                writer.right(546,baseline,amount(line.get("line_total")),10,false);
                float y=baseline;
                for(String part:name){if(y<90){writer.page();writer.tableHeader();y=writer.cursor-14;}writer.text(82,y,part,10,false);y-=14;}
                float bottom=y+14-9;
                writer.line(bottom);
                writer.cursor=bottom;
            }
            writer.cursor-=6;
            writer.space(240);
            writer.cursor-=12;
            writer.total("Descuento del pedido",amount(receipt.get("discount")),false);
            writer.total("Envío",amount(receipt.get("delivery_fee")),false);
            writer.total("Total del pedido",amount(receipt.get("order_total")),true);
            writer.cursor-=10;
            writer.total("Valor gravado de este pago",amount(receipt.get("subtotal")),false);
            writer.total("IGV incluido (18%)",amount(receipt.get("tax_amount")),false);
            writer.total("IMPORTE PAGADO",amount(receipt.get("total")),true);
            writer.total("Saldo pendiente del pedido",amount(receipt.get("pending_amount")),false);
            writer.cursor-=12;
            writer.paragraph("Los precios incluyen IGV. En pagos parciales, el detalle muestra la compra completa y el impuesto corresponde únicamente al importe de este pago. Las propinas se registran por separado.",false);
            writer.finish();
            document.getDocumentInformation().setTitle("Inti Crunch - "+receipt.get("series")+"-"+receipt.get("number"));
            document.save(output);return output.toByteArray();
        }catch(IOException exception){throw new IllegalStateException("No se pudo generar el comprobante PDF. El pago no se registró.",exception);}
    }
    private static String amount(Object value){return "S/ "+new BigDecimal(Objects.toString(value,"0")).setScale(2,java.math.RoundingMode.HALF_UP).toPlainString();}
    private static final class Writer {
        private final PDDocument document;
        private final Map<String,Object> receipt;
        private final PDFont regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream stream;
        private float cursor;
        Writer(PDDocument document,Map<String,Object> receipt){this.document=document;this.receipt=receipt;}
        void page()throws IOException{
            if(stream!=null){footer();stream.close();}
            PDPage page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);
            stream.setNonStrokingColor(new Color(199,70,32));stream.addRect(0,827,595,15);stream.fill();
            stream.setNonStrokingColor(new Color(199,70,32));stream.addRect(44,746,50,50);stream.fill();
            stream.setNonStrokingColor(Color.WHITE);rawText(54,762,"IC",24,bold);
            text(108,779,"INTI CRUNCH",23,true);text(109,760,"BRASA Y CORAZÓN · JULIACA",9,false);
            right(550,789,receipt.get("document_type")+" SIMULADA",12,true);
            right(550,771,receipt.get("series")+"-"+String.format("%08d",((Number)receipt.get("number")).longValue()),12,true);
            Object issued=receipt.get("issued_at");String date=issued instanceof java.sql.Timestamp timestamp?DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima")).format(timestamp.toInstant()):Objects.toString(issued,"");
            right(550,754,date+" · PEN",9,false);
            cursor=728;paragraph(Objects.toString(receipt.get("seller_name"),"Inti Crunch Juliaca S.A.C.")+" | RUC: "+Objects.toString(receipt.get("seller_tax_id"),"20000000001"),true);
            paragraph(Objects.toString(receipt.get("seller_address"),"Urb. La Rinconada, Juliaca"),false);
            paragraph("Teléfono: "+Objects.toString(receipt.get("seller_phone"),"951 000 999"),false);
            stream.setNonStrokingColor(new Color(255,242,222));stream.addRect(44,cursor-33,507,34);stream.fill();
            text(54,cursor-12,"DEMOSTRACIÓN · Datos ficticios · Pago simulado",10,true);
            text(54,cursor-25,"Sin validez tributaria. No enviado ni autorizado por SUNAT.",9,false);
            cursor-=54;
        }
        void footer()throws IOException{line(46);text(44,31,"Gracias por compartir nuestra brasa. Inti Crunch · Juliaca",9,false);right(550,31,"Página "+document.getNumberOfPages(),9,false);}
        void finish()throws IOException{footer();stream.close();stream=null;}
        void space(float height)throws IOException{if(cursor-height<65)page();}
        void rowSpace(float height)throws IOException{if(cursor-height<65){page();tableHeader();}}
        void paragraph(String value,boolean strong)throws IOException{for(String part:wrap(value,507,10)){space(15);text(44,cursor,part,10,strong);cursor-=15;}}
        void tableHeader()throws IOException{space(40);stream.setNonStrokingColor(new Color(43,38,30));stream.addRect(44,cursor-10,507,26);stream.fill();stream.setNonStrokingColor(Color.WHITE);rawText(49,cursor,"Cant.",10,bold);rawText(82,cursor,"Detalle del pedido",10,bold);rawText(402,cursor,"P. unitario",10,bold);rawText(500,cursor,"Importe",10,bold);cursor-=14;}
        void total(String label,String value,boolean strong)throws IOException{text(245,cursor,label,strong?11:10,strong);right(550,cursor,value,strong?12:10,strong);cursor-=22;}
        void line(float vertical)throws IOException{stream.setStrokingColor(new Color(226,220,211));stream.setLineWidth(0.5f);stream.moveTo(44,vertical);stream.lineTo(551,vertical);stream.stroke();}
        void right(float horizontal,float vertical,String value,float size,boolean strong)throws IOException{PDFont font=strong?bold:regular;String safe=safe(value);text(horizontal-font.getStringWidth(safe)/1000*size,vertical,safe,size,strong);}
        void text(float horizontal,float vertical,String value,float size,boolean strong)throws IOException{stream.setNonStrokingColor(new Color(43,38,30));rawText(horizontal,vertical,safe(value),size,strong?bold:regular);}
        void rawText(float horizontal,float vertical,String value,float size,PDFont font)throws IOException{stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(horizontal,vertical);stream.showText(safe(value));stream.endText();}
        List<String> wrap(String value,float width,float size)throws IOException{
            List<String> result=new ArrayList<>();String current="";
            for(String word:safe(value).split("\\s+")){
                String candidate=current.isEmpty()?word:current+" "+word;
                if(regular.getStringWidth(candidate)/1000*size<=width){current=candidate;continue;}
                if(!current.isEmpty())result.add(current);
                current="";
                for(char letter:word.toCharArray()){
                    if(regular.getStringWidth(current+letter)/1000*size>width){result.add(current);current="";}
                    current+=letter;
                }
            }
            if(!current.isEmpty())result.add(current);return result.isEmpty()?List.of(""):result;
        }
        String safe(String value){StringBuilder result=new StringBuilder();for(char letter:value.replace('\n',' ').replace('\r',' ').toCharArray()){try{regular.encode(String.valueOf(letter));result.append(letter);}catch(Exception exception){result.append('?');}}return result.toString();}
    }
}
