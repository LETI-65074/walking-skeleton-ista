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

import com.vaadin.flow.component.login.LoginOverlay;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.example.examplefeature.ui.TaskEmailSendSergiy;



import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("tasks-email")
@PageTitle("Send Email (read-only)")
@Menu(order = 1, icon = "vaadin:envelope", title = "Send Email (read-only)")
public class TaskEmailSendSergiy extends Main {

    private final TaskService taskService;
    private final Grid<Task> grid = new Grid<>();
    private final Button refreshBtn = new Button("Refresh");

    private EmailField recipient;
    private Button sendBtn;
    private Task selectedTask;              // guarda a tarefa selecionada

    private final LoginOverlay authOverlay = new LoginOverlay();
    private boolean authenticated = false;

    @Value("${app.auth.user:aluno}")
    private String authUser;

    @Value("${app.auth.pass:senha123}")
    private String authPass;

    // Mesmos formatadores usados na TaskListView
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(getLocale())
                    .withZone(ZoneId.systemDefault());


    private final EmailServiceSergiy emailService;
    public TaskEmailSendSergiy(TaskService taskService, EmailServiceSergiy emailService) {
        this.taskService = taskService;
        this.emailService = emailService;
                    // <- final é ok


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
        initGridSelection();
        setupEmailField();

// onde adicionas a toolbar, junta mais um "group" com os novos controlos:
        add(new ViewToolbar("Giimail",

                ViewToolbar.group(recipient, sendBtn)   // <- adiciona este grupo
        ));

        authOverlay.setTitle("Confirmação");
        authOverlay.setDescription("Autentica para enviar email");
        authOverlay.setForgotPasswordButtonVisible(false);
        authOverlay.addLoginListener(e -> {
            if (authUser.equals(e.getUsername()) && authPass.equals(e.getPassword())) {
                authenticated = true;
                System.out.println(authUser + " " + authPass + " login feito") ;
                authOverlay.setOpened(false);
                // chama o envio real agora que está autenticado
                doSendSelectedTask();  // método que envia de facto
            } else {
                authOverlay.setError(true);
            }
        });


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

        // validações
        if (to == null || to.isBlank() || recipient.isInvalid()) {
            Notification.show("Indica um destinatário válido.", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (selectedTask == null) {
            Notification.show("Seleciona uma tarefa na lista.", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }


        if (!authenticated) {
            authOverlay.setOpened(true);
        }


        doSendSelectedTask();
    }



    private void initGridSelection() {
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(ev -> {
            selectedTask = ev.getValue(); // pode ser null se desselecionar
        });
    }

    private void setupEmailField() {
        recipient.setRequiredIndicatorVisible(true);
        recipient.setErrorMessage("Indica um email válido");
        // EmailField já valida formato básico; se quiseres obrigar domínio da escola:
        // recipient.setPattern("^[^@\\s]+@iscte\\.pt$");
    }

    private void doSendSelectedTask() {
        String to = recipient.getValue();
        String due = Optional.ofNullable(selectedTask.getDueDate())
                .map(dateFormatter::format).orElse("Sem prazo");
        String subject = "Task selecionada";
        String body = "Descrição da tarefa: " + selectedTask.getDescription() + "\nPrazo: " + due;

        try {
            emailService.sendPlainText( to, subject, body);
            Notification.show("Email enviado para " + to, 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Falha ao enviar: " + ex.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }



}
