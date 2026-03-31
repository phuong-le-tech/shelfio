import http from './http';
import type { ActivityEvent, ActivityFilters, PageResponse } from '../types/activity';

const activityApi = {
  getActivity: (workspaceId: string, filters: ActivityFilters = {}) =>
    http
      .get<PageResponse<ActivityEvent>>(`/workspaces/${workspaceId}/activity`, {
        params: filters,
      })
      .then((r) => r.data),
};

export default activityApi;
