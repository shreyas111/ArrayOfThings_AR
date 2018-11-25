package aot.cs491.com.aot_ar.aothttpapi;

public class AOTResponse<E> {
    Object meta;
    E data;

    public Object getMeta() {
        return meta;
    }

    public void setMeta(Object meta) {
        this.meta = meta;
    }

    public E getData() {
        return data;
    }

    public void setData(E data) {
        this.data = data;
    }
}
