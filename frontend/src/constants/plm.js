// PLM domain constants — single source of truth for all status/type strings.
// Always import from here instead of hardcoding strings in components.

export const VERSION_STATUS = {
  DRAFT:      'DRAFT',
  IN_PROGRESS:'IN_PROGRESS',
  RELEASED:   'RELEASED',
  DEPRECATED: 'DEPRECATED',
  HOLD:       'HOLD',
};

export const CR_STATUS = {
  DRAFT:        'DRAFT',
  SUBMITTED:    'SUBMITTED',
  UNDER_REVIEW: 'UNDER_REVIEW',
  APPROVED:     'APPROVED',
  REJECTED:     'REJECTED',
  WITHDRAWN:    'WITHDRAWN',
};

export const CR_TYPE = {
  STANDARD: 'STANDARD',
  ENHANCED: 'ENHANCED',
};

export const APPROVAL_DECISION = {
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

export const PRODUCT_STATUS = {
  DRAFT:    'DRAFT',
  ACTIVE:   'ACTIVE',
  OBSOLETE: 'OBSOLETE',
};
