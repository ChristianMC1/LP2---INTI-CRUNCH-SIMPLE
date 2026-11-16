package pe.inticrunch.caja.entity;

import jakarta.persistence.*;

/** PDF guardado del comprobante. Tabla: payment_document_files */
@Entity
@Table(name = "payment_document_files")
public class ArchivoComprobante {
    @Id
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Lob  // BLOB en Oracle
    @Column(name = "content", nullable = false)
    private byte[] content;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
}
