package coordination;

import java.util.Collection;

public interface CoordinatorListener {
    void onStateUpdate(Collection<CoordinatedNode> nodes);
}
