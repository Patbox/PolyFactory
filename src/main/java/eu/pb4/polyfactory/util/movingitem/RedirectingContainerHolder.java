package eu.pb4.polyfactory.util.movingitem;

import org.jetbrains.annotations.Nullable;

public interface RedirectingContainerHolder extends ContainerHolder {
    void setContainerLocal(MovingItem container);

    @Nullable
    ContainerHolder getRedirect();

    @Override
    default void setContainer(MovingItem container) {
        this.setContainerLocal(container);
        var x = getRedirect();
        if (x != null) {
            x.setContainer(container);
        }
    }

    @Override
    default MovingItem pullAndRemove() {
        var redirect = getRedirect();
        if (redirect != null) {
            this.setContainerLocal(null);
            return redirect.pullAndRemove();
        }
        var x = this.getContainer();
        this.setContainerLocal(null);
        return x;
    }

    @Override
    default void pushAndAttach(MovingItem container) {
        this.setContainerLocal(container);

        var redirect = getRedirect();
        if (redirect != null) {
            redirect.pushAndAttach(container);
        }
    }
}
