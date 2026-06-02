package net.lenni0451.rivet.layer;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.container.Container;

import java.util.*;

@Accessors(fluent = true, chain = true, makeFinal = true)
public final class LayerList {

    @Getter
    private final Layer baseLayer;
    private final Map<LayerBucket, List<Layer>> layers = new EnumMap<>(LayerBucket.class);
    private List<Layer> allLayers = new ArrayList<>();

    public LayerList(final Rivet rivet, final Container baseContainer) {
        for (LayerBucket bucket : LayerBucket.values()) {
            this.layers.put(bucket, new ArrayList<>());
        }
        this.baseLayer = new Layer(baseContainer, LayerBucket.BASE);
        this.layers.get(LayerBucket.BASE).add(this.baseLayer);
        baseContainer.setRivet(rivet, this.baseLayer);
        this.updateAllLayers();
    }

    public List<Layer> get() {
        return Collections.unmodifiableList(this.allLayers);
    }

    public Layer findLayerAt(final float x, final float y) {
        for (int i = this.allLayers.size() - 1; i >= 0; i--) {
            Layer layer = this.allLayers.get(i);
            if (layer.bucket().interceptable() && layer.container().intercepts(x, y)) {
                return layer;
            }
        }
        return null;
    }

    public LayerList add(final Layer layer) {
        if (layer.bucket().equals(LayerBucket.BASE)) {
            throw new IllegalArgumentException("Cannot add a layer to the base bucket, base layer is already set");
        }
        this.layers.get(layer.bucket()).add(layer);
        this.updateAllLayers();
        return this;
    }

    public boolean remove(final Layer layer) {
        if (this.layers.get(layer.bucket()).remove(layer)) {
            this.updateAllLayers();
            return true;
        }
        return false;
    }

    private void updateAllLayers() {
        List<Layer> newAllLayers = new ArrayList<>();
        for (LayerBucket bucket : LayerBucket.values()) {
            newAllLayers.addAll(this.layers.get(bucket));
        }
        this.allLayers = newAllLayers;
    }

}
