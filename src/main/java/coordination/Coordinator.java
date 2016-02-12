package coordination;

import common.Service;

public interface Coordinator extends Service {
    void join(CoordinatedNode node);
    void leave(CoordinatedNode node);
    void subscribe(CoordinatorListener listener);
}
