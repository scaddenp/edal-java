/*******************************************************************************
 * Copyright (c) 2011 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package uk.ac.rdg.resc.edal.coverage;

import uk.ac.rdg.resc.edal.coverage.domain.GridDomain;
import uk.ac.rdg.resc.edal.coverage.grid.GridValuesMatrix;
import uk.ac.rdg.resc.edal.util.BigList;

/**
 * A {@link Coverage} whose domain is a {@link GridDomain}
 * 
 * @author Guy Griffiths
 * 
 * @param <P>
 *            The type of object used to identify positions within the
 *            coverage's domain. This may be a spatial, temporal, or combined
 *            spatiotemporal position.
 * @param <DO>
 *            The type of domain object
 */
public interface BaseGridCoverage<P, DO> extends DiscreteCoverage<P, DO> {

    @Override
    public GridDomain<P, DO> getDomain();

    /**
     * {@inheritDoc}
     * <p>
     * Grids can be large, so we specialize the return type to {@link BigList}.
     * </p>
     */
    @Override
    public BigList<Record> getValues();

    /**
     * {@inheritDoc}
     * <p>
     * Grids can be large, so we specialize the return type to {@link BigList}.
     * </p>
     */
    @Override
    public BigList<?> getValues(String memberName);

    /**
     * <p>
     * Gets an object through which the values of the given coverage member can
     * be accessed.
     * </p>
     * <p>
     * For disk-based storage, this method will usually return a new object with
     * each invocation. When the user has finished with the GridValuesMatrix,
     * close() must be called to free any resources. Following this, the
     * GridValuesMatrix may no longer be usable and a new one must be retrieved
     * using this method.
     * </p>
     * <p>
     * For in-memory storage, this method may choose to return the same object
     * with each invocation, and the call to GridValuesMatrix.close() may be
     * ignored.
     * </p>
     */
    public GridValuesMatrix<? extends Object> getGridValues(String memberName);
}