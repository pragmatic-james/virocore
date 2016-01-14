//
//  VROMatrix4f.h
//  ViroRenderer
//
//  Created by Raj Advani on 10/15/15.
//  Copyright © 2015 Viro Media. All rights reserved.
//

#ifndef VROMATRIX_H_
#define VROMATRIX_H_

#include <stdlib.h>
#include <math.h>

class VROVector3f;

class VROMatrix4f {
public:
    
    float &operator[] (const int index) {
        return _mtx[index];
    }
    float const &operator[](int index) const {
        return _mtx[index];
    }

    VROMatrix4f();
    VROMatrix4f(const float *matrix);
    virtual ~VROMatrix4f();

    void toIdentity();
    void copy(const VROMatrix4f &copy);

    /*
     Scale.
     */
    void scale(float x, float y, float z);

    /*
     Rotation.
     */
    void rotateX(float angleRad);
    void rotateY(float angleRad);
    void rotateZ(float angleRad);
    void rotate(float angleRad, const VROVector3f &origin, const VROVector3f &dir);
    void setRotationCenter(const VROVector3f &center, const VROVector3f &translation);

    /*
     Translation.
     */
    void translate(float x, float y, float z);
    void translate(const VROVector3f &vector);

    /*
     Multiplication.
     */
    VROMatrix4f multiply(const VROMatrix4f &matrix) const;
    VROVector3f multiply(const VROVector3f &vector) const;
    
    /*
     Other operations.
     */
    VROMatrix4f transpose() const;
    VROMatrix4f invert() const;
    const float *getArray() const {
        return _mtx;
    }
    
private:
    
    /*
     The 16-float data for this matrix.
     */
    float _mtx[16];
    
};

#endif /* VROMATRIX_H_ */
