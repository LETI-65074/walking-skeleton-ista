package com.example.examplefeature.ui;

import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.data.domain.PageRequest;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

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

        imprimir = new Button("Imprimir PDF", e -> imprimirAcao());
        imprimir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        imprimir.setEnabled(false);

        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        grid.addColumn(Task::getDescription).setHeader("Descrição").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(t -> t.getDueDate() == null ? "Never" : t.getDueDate().toString())
                .setHeader("Due Date").setAutoWidth(true);
        grid.addColumn(t -> t.getCreationDate().toString())
                .setHeader("Creation Date").setAutoWidth(true);
        grid.setSizeFull();

        grid.asSingleSelect().addValueChangeListener(e -> {
            var t = e.getValue();
            imprimir.setEnabled(imprimirTodas.getValue() || t != null);
            if (!imprimirTodas.getValue() && t != null && (nome.isEmpty() || nome.getValue().isBlank())) {
                nome.setValue(sanitize(t.getDescription() == null ? "tarefa" : t.getDescription()));
            }
        });
        add(nome, imprimirTodas, imprimir, grid);
        setSizeFull();
    }
    private void imprimirAcao() {
        try {
            String base = (nome.getValue() == null || nome.getValue().isBlank())
                    ? (imprimirTodas.getValue() ? "tarefas" : "tarefa")
                    : sanitize(nome.getValue());

            byte[] pdf;
            if (imprimirTodas.getValue()) {
                // busca “grande” simples (ajuste o tamanho se necessário)
                List<Task> tasks = taskService.list(PageRequest.of(0, 10_000));
                pdf = pdfService.tasksToPdf(tasks, getLocale());
            } else {
                Task t = grid.asSingleSelect().getValue();
                if (t == null) return; // nada selecionado
                if (nome.isEmpty() || nome.getValue().isBlank()) {
                    base = sanitize(t.getDescription() == null ? "tarefa" : t.getDescription());
                }
                pdf = pdfService.taskToPdf(t, getLocale());
            }

            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String niceName = base + "_" + ts + ".pdf";

            Anchor a = new Anchor(ev -> {
                ev.setContentType("application/pdf"); // inline
                try (OutputStream os = ev.getOutputStream()) { os.write(pdf); }
            }, "");
            a.getElement().setAttribute("hidden", true);
            add(a);

            String url = a.getHref() + (a.getHref().contains("?") ? "&" : "?") + "name=" + niceName;
            UI.getCurrent().getPage().executeJs(
                    "const u=$0,i=document.createElement('iframe');i.style.display='none';i.src=u;i.onload=()=>{try{i.contentWindow.print()}catch(e){} setTimeout(()=>i.remove(),2000)};document.body.appendChild(i);",
                    url
            );

            //getUI().ifPresent(ui -> ui.accessLater(a::remove));
            nome.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private String sanitize(String s) {return s.strip().replaceAll("[^\\p{L}\\p{N}_-]+", "_"); }
}
