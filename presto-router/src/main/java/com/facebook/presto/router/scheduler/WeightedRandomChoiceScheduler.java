/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.router.scheduler;

import com.facebook.airlift.log.Logger;
import com.facebook.presto.spi.router.RouterRequestInfo;
import com.facebook.presto.spi.router.Scheduler;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.nCopies;

public class WeightedRandomChoiceScheduler
        implements Scheduler
{
    private List<URI> candidates;
    private Map<URI, Integer> weights;

    private static final Random RANDOM = new Random();
    private static final Logger log = Logger.get(WeightedRandomChoiceScheduler.class);

    @Override
    public Optional<URI> getDestination(RouterRequestInfo routerRequestInfo)
    {
        checkArgument(candidates.size() == weights.size());

        try {
            List<URI> serverList = weights.keySet().stream()
                    .map(uri -> nCopies(weights.get(uri), uri))
                    .flatMap(Collection::stream)
                    .collect(toImmutableList());

            //If server list is empty (servers got filtered out due to 0 weight)
            //select the first candidate from candidate list
            if (serverList.isEmpty() && !candidates.isEmpty()) {
                return Optional.of(candidates.get(0));
            }

            return Optional.of(serverList.get(RANDOM.nextInt(serverList.size())));
        }
        catch (IllegalArgumentException e) {
            log.warn(e, "Error getting destination for user " + routerRequestInfo.getUser());
            return Optional.empty();
        }
    }

    public void setCandidates(List<URI> candidates)
    {
        this.candidates = candidates;
    }

    public void setWeights(Map<URI, Integer> weights)
    {
        this.weights = weights;
    }

    public Map<URI, Integer> getWeights()
    {
        return weights;
    }
}
