//
//  VROMaterialSubstrateMetal.h
//  ViroRenderer
//
//  Created by Raj Advani on 11/30/15.
//  Copyright © 2015 Viro Media. All rights reserved.
//

#ifndef VROMaterialSubstrateMetal_h
#define VROMaterialSubstrateMetal_h

#include "VROMaterial.h"
#include <Metal/Metal.h>
#include <MetalKit/MetalKit.h>
#include <vector>
#include "VROMaterialSubstrate.h"

class VROMatrix4f;
class VROVector4f;
class VRODriverMetal;
class VROLight;
class VROConcurrentBuffer;
class VRORenderParameters;
enum class VROEyeType;

/*
 Metal representation of a VROMaterial. Each VROMaterial defines a vertex
 program and fragment program (by way of the material's lighting model), along
 with the set of uniforms and samplers to bind to said program.
 */
class VROMaterialSubstrateMetal : public VROMaterialSubstrate {
    
public:
    
    VROMaterialSubstrateMetal(const VROMaterial &material,
                              const VRODriverMetal &driver);
    virtual ~VROMaterialSubstrateMetal();
    
    /*
     Set the uniforms required to render this material, and return the buffer.
     */
    VROConcurrentBuffer &bindMaterialUniforms(VRORenderParameters &params, VROEyeType eye, int frame);
    
    /*
     Set the uniforms required to render this given material under the
     given lights, and return the buffer.
     */
    VROConcurrentBuffer &bindLightingUniforms(const std::vector<std::shared_ptr<VROLight>> &lights,
                                              VROEyeType eye, int frame);
    
    id <MTLFunction> getVertexProgram() const {
        return _vertexProgram;
    }
    id <MTLFunction> getFragmentProgram() const {
        return _fragmentProgram;
    }
    const std::vector<std::shared_ptr<VROTexture>> &getTextures() const {
        return _textures;
    }
    
    void updateSortKey(VROSortKey &key) const;
    
private:
    
    const VROMaterial &_material;
    VROLightingModel _lightingModel;
    
    id <MTLFunction> _vertexProgram;
    id <MTLFunction> _fragmentProgram;
    
    VROConcurrentBuffer *_materialUniformsBuffer;
    VROConcurrentBuffer *_lightingUniformsBuffer;
    
    std::vector<std::shared_ptr<VROTexture>> _textures;
    
    void loadConstantLighting(const VROMaterial &material,
                              id <MTLLibrary> library, id <MTLDevice> device,
                              const VRODriverMetal &driver);
    void loadBlinnLighting(const VROMaterial &material,
                           id <MTLLibrary> library, id <MTLDevice> device,
                           const VRODriverMetal &driver);
    void loadPhongLighting(const VROMaterial &material,
                           id <MTLLibrary> library, id <MTLDevice> device,
                           const VRODriverMetal &driver);
    void loadLambertLighting(const VROMaterial &material,
                             id <MTLLibrary> library, id <MTLDevice> device,
                             const VRODriverMetal &driver);
    
    void bindConstantLighting(const std::shared_ptr<VROLight> &light);
    void bindBlinnLighting(const std::shared_ptr<VROLight> &light);
    void bindPhongLighting(const std::shared_ptr<VROLight> &light);
    void bindLambertLighting(const std::shared_ptr<VROLight> &light);
    
};

#endif /* VROMaterialSubstrateMetal_h */
