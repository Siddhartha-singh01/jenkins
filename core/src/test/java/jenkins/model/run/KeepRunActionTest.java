package jenkins.model.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Action;
import hudson.model.Run;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("rawtypes")
class KeepRunActionTest {

    private Run run;
    private KeepRunAction factory;

    @BeforeEach
    void setUp() {
        run = mock(Run.class);
        factory = new KeepRunAction();
        System.setProperty("new-build-page.flag.defaultValue", "true");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("new-build-page.flag.defaultValue");
    }

    @Test
    void keepActionRequiresRunUpdatePermission() {
        when(run.canToggleLogKeep()).thenReturn(true);
        when(run.isKeepLog()).thenReturn(false);
        when(run.hasPermission(Run.UPDATE)).thenReturn(true);

        Collection<? extends Action> actions = factory.createFor(run);
        assertEquals(1, actions.size());
    }

    @Test
    void keepActionHiddenWithoutUpdatePermission() {
        when(run.canToggleLogKeep()).thenReturn(true);
        when(run.isKeepLog()).thenReturn(false);

        Collection<? extends Action> actions = factory.createFor(run);
        assertTrue(actions.isEmpty());
    }

    @Test
    void unkeepActionRequiresRunDeletePermission() {
        when(run.canToggleLogKeep()).thenReturn(true);
        when(run.isKeepLog()).thenReturn(true);
        when(run.hasPermission(Run.DELETE)).thenReturn(true);

        Collection<? extends Action> actions = factory.createFor(run);
        assertEquals(1, actions.size());
    }

    @Test
    void unkeepActionHiddenWithoutDeletePermission() {
        when(run.canToggleLogKeep()).thenReturn(true);
        when(run.isKeepLog()).thenReturn(true);

        Collection<? extends Action> actions = factory.createFor(run);
        assertTrue(actions.isEmpty());
    }

    @Test
    void noActionsWhenLogKeepCannotToggle() {
        when(run.canToggleLogKeep()).thenReturn(false);

        Collection<? extends Action> actions = factory.createFor(run);
        assertTrue(actions.isEmpty());
    }
}
