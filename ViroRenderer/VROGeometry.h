//
//  VROGeometry.h
//  ViroRenderer
//
//  Created by Raj Advani on 11/17/15.
//  Copyright © 2015 Viro Media. All rights reserved.
//

#ifndef VROGeometry_h
#define VROGeometry_h

#include <stdio.h>
#include <vector>
#include <string>
#include <memory>
#include "VRORenderContext.h"
#include "VRODriver.h"
#include "VROSortKey.h"
#include "VROBoundingBox.h"
#include "VROAllocationTracker.h"

class VRONode;
class VROLight;
class VROSortKey;
class VROMaterial;
class VROGeometryElement;
class VROGeometrySource;
class VROGeometrySubstrate;
class VROMatrix4f;
class VRORenderParameters;
enum class VROGeometrySourceSemantic;

/*
 Represents a three-dimensional shape, a collection of vertices, normals and texture coordinates
 that define a surface, also known as a model or mesh. Geometries attached to VRONode objects form
 the visible elements of a scene, and VROMaterial objects attached to a geometry determine its 
 appearance.
 */
class VROGeometry {
    
public:
    
    /*
     Construct a new geometry with the given sources and elements. If a context
     is supplied, the geometry will be prewarmed.
     */
    VROGeometry(std::vector<std::shared_ptr<VROGeometrySource>> sources,
                std::vector<std::shared_ptr<VROGeometryElement>> elements,
                const VRODriver *driver = nullptr) :
        _renderingOrder(0),
        _geometrySources(sources),
        _geometryElements(elements),
        _stereoRenderingEnabled(true),
        _bounds(nullptr),
        _substrate(nullptr) {
            
        if (driver) {
            prewarm(*driver);
        }
            
         ALLOCATION_TRACKER_ADD(Geometry, 1);
    }
    
    /*
     Copy the given geometry. The materials will *not* be copied, and the
     underlying immutable geometry data will be shared.
     */
    VROGeometry(std::shared_ptr<VROGeometry> geometry) :
        _renderingOrder(geometry->_renderingOrder),
        _geometrySources(geometry->_geometrySources),
        _geometryElements(geometry->_geometryElements) {
        
         ALLOCATION_TRACKER_ADD(Geometry, 1);
    }
    
    ~VROGeometry();
    
    /*
     Get the geometry ready for usage now, in advance of when it's visible. If not invoked,
     the geometry will be initialized when it is made visible.
     */
    void prewarm(const VRODriver &driver);

    void render(int elementIndex,
                std::shared_ptr<VROMaterial> &material,
                VROMatrix4f transform,
                float opacity,
                const VRORenderContext &context,
                const VRODriver &driver);
    
    void updateSortKeys(VRONode *node, uint32_t lightsHash);
    void getSortKeys(std::vector<VROSortKey> *outKeys);
    
    std::shared_ptr<VROMaterial> &getMaterialForElement(int elementIndex) {
        return _materials[elementIndex % _materials.size()];
    }
    
    std::vector<std::shared_ptr<VROMaterial>> &getMaterials() {
        return _materials;
    }
    const std::vector<std::shared_ptr<VROMaterial>> &getMaterials_const() const {
        return _materials;
    }
    
    const std::vector<std::shared_ptr<VROGeometrySource>> &getGeometrySources() const {
        return _geometrySources;
    }
    const std::vector<std::shared_ptr<VROGeometryElement>> &getGeometryElements() const {
        return _geometryElements;
    }
    
    const VROBoundingBox &getBoundingBox();
    VROVector3f getCenter();
    
    bool isStereoRenderingEnabled() const {
        return _stereoRenderingEnabled;
    }
    void setStereoRenderingEnabled(bool enabled) {
        _stereoRenderingEnabled = enabled;
    }
    
    std::vector<std::shared_ptr<VROGeometrySource>> getGeometrySourcesForSemantic(VROGeometrySourceSemantic semantic) const;
    
private:
    
    /*
     User-assigned name of this geometry.
     */
    std::string _name;
    
    /*
     User-defined rendering order for this geometry.
     */
    int _renderingOrder;
    
    /*
     The materials, which define the surface appearance (color, lighting, texture, and effects)
     of each geometry element.
     
     If a geometry has the same number of materials as it has geometry elements, the material 
     index corresponds to the element index. For geometries with fewer materials than elements,
     the material index for each element is determined by calculating the index of that element
     modulo the number of materials. For example, in a geometry with six elements and three materials, 
     the element at index 5 is rendered using the material at index 5 % 3 = 2.
     */
    std::vector<std::shared_ptr<VROMaterial>> _materials;
    const std::vector<std::shared_ptr<VROGeometrySource>> _geometrySources;
    const std::vector<std::shared_ptr<VROGeometryElement>> _geometryElements;
    
    /*
     Used for sorting the elements prior to rendering.
     */
    std::vector<VROSortKey> _sortKeys;
    
    /*
     True if stereo rendering is enabled for this object. If disabled, the monocular view matrix
     will be used when rendering. Stereo rendering should be disabled for nearby objects that 
     simulate being far away, like skyboxes.
     */
    bool _stereoRenderingEnabled;
    
    /*
     The bounding box of this geometry. Created on demand, then cached.
     */
    VROBoundingBox *_bounds;
    
    /*
     Representation of this geometry in the underlying graphics library.
     */
    VROGeometrySubstrate *_substrate;
    
};

#endif /* VROGeometry_h */
