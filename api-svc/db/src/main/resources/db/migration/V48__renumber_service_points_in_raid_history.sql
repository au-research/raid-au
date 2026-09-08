-- Finish the renumbering V46 started, for raid_history.
--
-- V46 moved Service Points into the block allocated to this Registration Agency
-- and rewrote the id everywhere it is stored except raid_history, which holds the
-- JSON Patch documents RaidHistoryService replays to reconstruct a RaidDto. That
-- reconstruction backs the single-raid GET, the edit form and the update
-- checksum, so patches carrying a pre-renumbering id make the detail view
-- disagree with the row it belongs to and break updates, which look the Service
-- Point up by the id the patches carry.
--
-- This is a separate migration rather than an amendment to V46 because V46 has
-- already been applied elsewhere, and Flyway never re-runs an applied migration:
-- changing it would only move its checksum, leaving the data untouched.
--
-- diff is text rather than jsonb and holds an array of operations, so this
-- rebuilds each array rather than setting a fixed path. The id can sit at three
-- depths depending on how much of the document a given revision rewrote, and op
-- is not part of the match because add and replace need identical treatment.

do
$$
    declare
        v_start      bigint := ${servicePointIdStart};
        v_block      bigint := ${servicePointIdBlockSize};
        v_offsets    bigint[];
        v_offset     bigint;
        v_out_of_block bigint;
    begin
        -- V46 shifted every Service Point by one offset, so recovering that offset
        -- is enough to finish the job. It cannot be derived from service_point
        -- alone, which now holds only post-renumbering ids, nor from the lowest
        -- surviving patch value, which need not belong to the lowest Service
        -- Point. It is recovered instead by pairing a stale patch with the raid
        -- that owns it, since raid.service_point_id was renumbered by V46 and is
        -- authoritative.
        --
        -- A raid can change Service Point, so more than one offset means the
        -- pairing is ambiguous and the data needs a human rather than a guess.
        select array_agg(distinct r.service_point_id - stale.service_point)
        into v_offsets
        from raid_history h
                 join raid r on r.handle = h.handle,
             lateral jsonb_array_elements(h.diff::jsonb) as element,
             lateral (select case
                                 when element ->> 'path' = '/identifier/owner/servicePoint'
                                     and jsonb_typeof(element -> 'value') = 'number'
                                     then (element ->> 'value')::bigint
                                 when element ->> 'path' = '/identifier/owner'
                                     and element -> 'value' ? 'servicePoint'
                                     then (element -> 'value' ->> 'servicePoint')::bigint
                                 when element ->> 'path' = '/identifier'
                                     and element -> 'value' -> 'owner' ? 'servicePoint'
                                     then (element -> 'value' -> 'owner' ->> 'servicePoint')::bigint
                                 end) as stale(service_point)
        where jsonb_typeof(h.diff::jsonb) = 'array'
          and stale.service_point is not null
          and (stale.service_point < v_start or stale.service_point >= v_start + v_block);

        if v_offsets is null then
            raise notice 'No raid_history patches carry a pre-renumbering Service Point; nothing to do';
            return;
        end if;

        if array_length(v_offsets, 1) > 1 then
            raise exception 'raid_history patches imply % different renumbering offsets (%); resolve by hand',
                array_length(v_offsets, 1), v_offsets;
        end if;

        v_offset := v_offsets[1];

        raise notice 'Shifting out-of-block Service Points in raid_history by %', v_offset;

        -- Only out-of-block values are shifted, so patches that already hold a
        -- renumbered id are left exactly as they are.
        update raid_history
        set diff = (select jsonb_agg(
                                   case
                                       when element ->> 'path' = '/identifier/owner/servicePoint'
                                           and jsonb_typeof(element -> 'value') = 'number'
                                           and ((element ->> 'value')::bigint < v_start
                                               or (element ->> 'value')::bigint >= v_start + v_block)
                                           then jsonb_set(element, '{value}',
                                                          to_jsonb((element ->> 'value')::bigint + v_offset))
                                       when element ->> 'path' = '/identifier/owner'
                                           and element -> 'value' ? 'servicePoint'
                                           and ((element -> 'value' ->> 'servicePoint')::bigint < v_start
                                               or (element -> 'value' ->> 'servicePoint')::bigint >= v_start + v_block)
                                           then jsonb_set(element, '{value,servicePoint}',
                                                          to_jsonb((element -> 'value' ->> 'servicePoint')::bigint +
                                                                   v_offset))
                                       when element ->> 'path' = '/identifier'
                                           and element -> 'value' -> 'owner' ? 'servicePoint'
                                           and ((element -> 'value' -> 'owner' ->> 'servicePoint')::bigint < v_start
                                               or (element -> 'value' -> 'owner' ->> 'servicePoint')::bigint >=
                                                  v_start + v_block)
                                           then jsonb_set(element, '{value,owner,servicePoint}',
                                                          to_jsonb((element -> 'value' -> 'owner' ->> 'servicePoint')::bigint +
                                                                   v_offset))
                                       else element
                                       end
                                   order by ordinality)::text
                    from jsonb_array_elements(diff::jsonb) with ordinality as patch(element, ordinality))
        where jsonb_typeof(diff::jsonb) = 'array'
          and diff like '%servicePoint%';

        -- No foreign key protects these patches, so assert the rewrite reached
        -- every one. Orphaned history, whose raid has since been deleted, is
        -- covered too: it was shifted by the same offset and must land in block
        -- like everything else.
        select count(*)
        into v_out_of_block
        from raid_history h,
             lateral jsonb_array_elements(h.diff::jsonb) as element,
             lateral (select case
                                 when element ->> 'path' = '/identifier/owner/servicePoint'
                                     and jsonb_typeof(element -> 'value') = 'number'
                                     then (element ->> 'value')::bigint
                                 when element ->> 'path' = '/identifier/owner'
                                     and element -> 'value' ? 'servicePoint'
                                     then (element -> 'value' ->> 'servicePoint')::bigint
                                 when element ->> 'path' = '/identifier'
                                     and element -> 'value' -> 'owner' ? 'servicePoint'
                                     then (element -> 'value' -> 'owner' ->> 'servicePoint')::bigint
                                 end) as sp(service_point)
        where jsonb_typeof(h.diff::jsonb) = 'array'
          and sp.service_point is not null
          and (sp.service_point < v_start or sp.service_point >= v_start + v_block);

        if v_out_of_block > 0 then
            raise exception 'Renumbering left % raid_history patches outside the allocated block', v_out_of_block;
        end if;
    end
$$;
