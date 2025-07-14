import { WebPlugin } from '@capacitor/core';

import type { BarcodePlugin } from './definitions';

export class BarcodeWeb extends WebPlugin implements BarcodePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
