package com.mail.springbootimaplistener.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "config")
public class Config implements Serializable {

    @Id
    @Column(name = "name")
    private String configKey;
    
    @Column(name = "value")
    private String configValue;
    
}
