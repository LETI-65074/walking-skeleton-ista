package com.example.examplefeature.ui;

import com.example.base.ui.component.ViewToolbar;
import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("tasks-email")
@PageTitle("Send Email (read-only)")
@Menu(order = 1, icon = "vaadin:envelope", title = "Send Email (read-only)")
public class TaskEmailSend extends Main {

    private final TaskService taskService;
    private final Grid<Task> grid = new Grid<>();
    private final Button refreshBtn = new Button("Refresh");

    private EmailField recipient;
    private Button sendBtn;

    // Mesmos formatadores usados na TaskListView
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(getLocale())
                    .withZone(ZoneId.systemDefault());

    public TaskEmailSend(TaskService taskService) {
        this.taskService = taskService;

        setSizeFull();
        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        // Colunas (Description, Due Date, Creation Date)
        grid.addColumn(Task::getDescription)
                .setHeader("Description")
                .setAutoWidth(true);

        grid.addColumn(task ->
                Optional.ofNullable(task.getDueDate())
                        .map(dateFormatter::format)  // LocalDate -> format direto
                        .orElse("Never")
        ).setHeader("Due Date").setAutoWidth(true);

        grid.addColumn(task ->
                dateTimeFormatter.format(task.getCreationDate()) // Instant/ZonedDateTime -> format com zona
        ).setHeader("Creation Date").setAutoWidth(true);

        grid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        grid.setSizeFull();

        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshBtn.addClickListener(e -> grid.getDataProvider().refreshAll());



        initEmailControls(); // <- chama antes de construir a toolbar

// onde adicionas a toolbar, junta mais um "group" com os novos controlos:
        add(new ViewToolbar("Giimail",

                ViewToolbar.group(recipient, sendBtn)   // <- adiciona este grupo
        ));

        add(grid);
    }

    private void initEmailControls() {
        recipient = new EmailField("Enviar para");
        recipient.setPlaceholder("alguem@exemplo.com");
        recipient.setClearButtonVisible(true);
        recipient.setWidth("22em");
        recipient.setRequiredIndicatorVisible(true);

        sendBtn = new Button("Enviar", e -> onSendClick());
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    private void onSendClick() {
        String to = recipient.getValue();
        if (to == null || to.isBlank() || !recipient.isInvalid() && !recipient.isEmpty() && !recipient.getValue().contains("@")) {
            // validação simples — podes trocar por recipient.isInvalid() se usares setPattern(...)
        }
        if (to == null || to.isBlank()) {
            Notification.show("Indica um destinatário.", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Notification.show("OK: (simulação) enviar para " + to, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }


}
