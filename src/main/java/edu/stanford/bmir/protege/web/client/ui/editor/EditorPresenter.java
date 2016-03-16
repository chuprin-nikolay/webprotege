package edu.stanford.bmir.protege.web.client.ui.editor;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceCallback;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInHandler;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutHandler;
import edu.stanford.bmir.protege.web.client.permissions.LoggedInUserProjectPermissionChecker;
import edu.stanford.bmir.protege.web.shared.HasDispose;
import edu.stanford.bmir.protege.web.shared.dispatch.GetObjectAction;
import edu.stanford.bmir.protege.web.shared.dispatch.GetObjectResult;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;
import edu.stanford.bmir.protege.web.shared.dispatch.UpdateObjectAction;
import edu.stanford.bmir.protege.web.shared.event.HandlerRegistrationManager;
import edu.stanford.bmir.protege.web.shared.event.PermissionsChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.PermissionsChangedHandler;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

import java.io.Serializable;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 23/04/2013
 */
public class EditorPresenter implements HasDispose {

    private static final Label NOTHING_SELECTED_WIDGET = new Label("Nothing selected");

    private static final Label LOADING_INDICATOR_WIDGET = new Label("Loading...");

    private static final int VALUE_CHANGED_COMMIT_DELAY_MS = 2000;

    private final DispatchServiceManager dispatchServiceManager;

    private final LoggedInUserProjectPermissionChecker permissionChecker;

    private final ContextMapper contextMapper;


    private final SimplePanel editorHolder = new SimplePanel();

    private Optional<EditorState<?, ?>> lastEditorState = Optional.absent();

    private HandlerRegistration valueChangedReg;


    private HandlerManager handlerManager = new HandlerManager(this);


    private int counter = 0;

    private HandlerRegistrationManager handlerRegistrationManager;

    private Timer commitOnValueChangedTimer = new Timer() {
        @Override
        public void run() {
            commitCurrentValue(lastEditorState.get());
        }
    };

    @Inject
    public EditorPresenter(ProjectId projectId, EventBus eventBus, DispatchServiceManager dispatchServiceManager, ContextMapper contextMapper, LoggedInUserProjectPermissionChecker permissionChecker) {
        this.contextMapper = contextMapper;
        this.dispatchServiceManager = dispatchServiceManager;
        this.permissionChecker = permissionChecker;
        this.handlerRegistrationManager = new HandlerRegistrationManager(eventBus);

        handlerRegistrationManager.registerHandlerToProject(projectId, PermissionsChangedEvent.TYPE, new PermissionsChangedHandler() {
            @Override
            public void handlePersmissionsChanged(PermissionsChangedEvent event) {
                updatePermissionBasedItems();
            }
        });
        handlerRegistrationManager.registerHandler(UserLoggedInEvent.TYPE, new UserLoggedInHandler() {
            @Override
            public void handleUserLoggedIn(UserLoggedInEvent event) {
                updatePermissionBasedItems();
            }
        });
        handlerRegistrationManager.registerHandler(UserLoggedOutEvent.TYPE, new UserLoggedOutHandler() {
            @Override
            public void handleUserLoggedOut(UserLoggedOutEvent event) {
                updatePermissionBasedItems();
            }
        });
    }

    public HandlerRegistration addEditorContextChangedHandler(EditorContextChangedHandler handler) {
        return handlerManager.addHandler(EditorContextChangedEvent.TYPE, handler);
    }

    public Widget getView() {
        return editorHolder;
    }


    public <C extends EditorCtx> void setEditorContext(final Optional<C> editorContext) {
        if (lastEditorState.isPresent()) {
            unbindPrevious(lastEditorState.get());
        }
        if (editorContext.isPresent()) {
            bindNext(editorContext.get());
        }
        else {
            editorHolder.setWidget(NOTHING_SELECTED_WIDGET);
        }
    }

    private void clearEditorState() {
        lastEditorState = Optional.absent();
    }

    private <C extends EditorCtx, O extends Serializable> void setEditorState(O pristineObject, C editorContext, EditorManager<C, O> editorManager) {
        lastEditorState = Optional.<EditorState<?, ?>>of(new EditorState<C, O>(pristineObject, editorContext, editorManager));
        handlerManager.fireEvent(new EditorContextChangedEvent(editorContext, editorManager.getDescription(editorContext)));
    }


    private <C extends EditorCtx, O extends Serializable> void unbindPrevious(EditorState<C, O> editorState) {
        valueChangedReg.removeHandler();
        commitOnValueChangedTimer.cancel();
        if (lastEditorState.isPresent()) {
            commitCurrentValue(editorState);
        }
        clearEditorState();
    }

    /**
     * Commit the current value
     *
     * @param editorState The state to be committed
     */
    private <C extends EditorCtx, O extends Serializable> void commitCurrentValue(final EditorState<C, O> editorState) {
        if (!editorState.getEditorManager().getView(editorState.getEditorContext()).isDirty()) {
            return;
        }
        final Optional<O> value = editorState.getEditedValue();
        if (!value.isPresent()) {
            return;
        }
        final O pristineValue = editorState.getPristineObject();
        final O editedValue = value.get();
        if (pristineValue.equals(editedValue)) {
            return;
        }
        final C editorCtx = editorState.getEditorContext();
        EditorManager<C, O> editorManager = editorState.getEditorManager();
        UpdateObjectAction<O> updateAction = editorManager.createUpdateObjectAction(pristineValue, editedValue, editorCtx);
        setEditorState(editedValue, editorCtx, editorManager);
        dispatchServiceManager.execute(updateAction, new DispatchServiceCallback<Result>() {
        });
    }


    private <C extends EditorCtx, O extends Serializable> void bindNext(final C editorCtx) {
        counter++;
        final Optional<EditorManager<C, O>> selectedMan = contextMapper.getEditorManager(editorCtx);
        if (selectedMan.isPresent()) {
            final EditorManager<C, O> editorManager = selectedMan.get();
            GetObjectAction<O> action = editorManager.createGetObjectAction(editorCtx);
            updatePermissionBasedItems();
            dispatchServiceManager.execute(action, new DispatchServiceCallback<GetObjectResult<O>>() {

                private int executionCounter = counter;

                @Override
                public void handleSuccess(GetObjectResult<O> result) {
                    handleGetObjectSuccess(result);
                }

                private void handleGetObjectSuccess(GetObjectResult<O> result) {
                    if (executionCounter != counter) {
                        return;
                    }
                    final O value = result.getObject();
                    final EditorView<O> editorView = editorManager.getView(editorCtx);
                    editorView.setValue(value);
                    valueChangedReg = editorView.addValueChangeHandler(new ValueChangeHandler<Optional<O>>() {
                        @Override
                        public void onValueChange(ValueChangeEvent<Optional<O>> event) {
                            rescheduleCommit();
                        }
                    });
                    final Widget editorWidget = editorView.asWidget();
                    editorHolder.setWidget(editorWidget);
                    setEditorState(value, editorCtx, editorManager);
                }
            });
        }
        else {
            editorHolder.setWidget(new Label("No editor available for selection: " + editorCtx));
        }
    }

    private void rescheduleCommit() {
        commitOnValueChangedTimer.cancel();
        commitOnValueChangedTimer.schedule(VALUE_CHANGED_COMMIT_DELAY_MS);
    }

    @Override
    public void dispose() {
        handlerRegistrationManager.removeHandlers();
    }


    public void updatePermissionBasedItems() {
        if (editorHolder.getWidget() instanceof HasEnabled) {
            final HasEnabled hasEnabled = (HasEnabled) editorHolder.getWidget();
            hasEnabled.setEnabled(false);
            permissionChecker.hasWritePermission(new DispatchServiceCallback<Boolean>() {
                @Override
                public void handleSuccess(Boolean result) {
                    hasEnabled.setEnabled(result);
                }

                @Override
                public void handleErrorFinally(Throwable throwable) {
                    GWT.log("[EditorPresenter] An error occurred updating the permission based items");
                }
            });
        }
    }


    private static class EditorState<C extends EditorCtx, O extends Serializable> {

        private final O pristineObject;

        private final C editorContext;

        private final EditorManager<C, O> editorManager;

        private EditorState(O pristineObject, C editorContext, EditorManager<C, O> editorManager) {
            this.pristineObject = pristineObject;
            this.editorContext = editorContext;
            this.editorManager = editorManager;
        }

        public O getPristineObject() {
            return pristineObject;
        }

        public C getEditorContext() {
            return editorContext;
        }

        public EditorManager<C, O> getEditorManager() {
            return editorManager;
        }

        public Optional<O> getEditedValue() {
            return editorManager.getView(editorContext).getValue();
        }
    }


}
