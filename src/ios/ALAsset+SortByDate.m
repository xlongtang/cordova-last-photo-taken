//
//  ALAsset+SortByDate.m
//  NXdrive
//
//  Created by Xiaolong Tang on 2/12/15.
//
//

#import "ALAsset+SortByDate.h"

@implementation ALAsset (SortByDate)

- (NSDate *) date
{
    return [self valueForProperty:ALAssetPropertyDate];
}

@end
