package com.nbbang.api;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.nbbang.service.InstitutionCatalog;
import com.nbbang.service.InstitutionCatalog.View;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
    private final InstitutionCatalog catalog;
    public InstitutionController(InstitutionCatalog catalog){this.catalog=catalog;}
    @GetMapping public List<View> list(){return catalog.list();}
}
