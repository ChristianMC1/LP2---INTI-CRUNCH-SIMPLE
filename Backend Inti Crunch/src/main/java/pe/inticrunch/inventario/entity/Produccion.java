package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Porciones preparadas por adelantado. Tabla: production_runs */
@Entity
@Table(name = "production_runs")
public class Produccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "planned_servings", nullable = false)
    private Integer plannedServings;

    @Column(name = "produced_servings", nullable = false)
    private Integer producedServings = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public Integer getPlannedServings() { return plannedServings; }
    public void setPlannedServings(Integer plannedServings) { this.plannedServings = plannedServings; }
    public Integer getProducedServings() { return producedServings; }
    public void setProducedServings(Integer producedServings) { this.producedServings = producedServings; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
