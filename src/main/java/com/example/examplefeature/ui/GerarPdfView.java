package com.example.examplefeature.ui;

import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("gerar-pdf")
@PageTitle("Gerar PDF")
@Menu(order = 1, icon = "vaadin:file-text", title = "Gerar PDF")
class GerarPdfView extends Main {

    private final TaskService taskService;
    private final PdfService pdfService;

    private Grid<Task> grid;
    private final TextField nome;
    private Checkbox imprimirTodas;
    private Button imprimir = null;

    GerarPdfView(TaskService taskService, PdfService pdfService) {
        this.taskService = taskService;
        this.pdfService = pdfService;

        nome = new TextField("Nome do ficheiro");
        nome.setPlaceholder("ex.: Tarefas");
        nome.setClearButtonVisible(true);
        nome.setMinWidth("16em");

        imprimirTodas = new Checkbox("Imprimir todas");
        imprimirTodas.addValueChangeListener(e -> {
            // se marcar "todas", não precisamos de seleção
            imprimir.setEnabled(e.getValue() || grid.asSingleSelect().getValue() != null);
        });
    }
}
