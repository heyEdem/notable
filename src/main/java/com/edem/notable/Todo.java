package com.edem.notable;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Todo {

    private Long id;
    private String title;
    private String description;
    private boolean completed;
}
