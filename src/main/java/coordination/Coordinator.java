package coordination;

public interface Coordinator {
    void join(CoordinatedNode node);
    void leave(CoordinatedNode node);
    void subscribe(CoordinatorListener listener);
}
